"""
Hardware-accelerated Telegram crypto module for ARC on Android.
Binds Pyrogram's MTProto AES-IGE and AES-CTR ciphers to native Android javax.crypto.Cipher.
"""
__version__ = "1.2.5"

import logging

log = logging.getLogger(__name__)

try:
    from com.erawat.arc import ArcCrypto

    def ige256_decrypt(data: bytes, key: bytes, iv: bytes) -> bytes:
        return bytes(ArcCrypto.ige256Decrypt(data, key, iv))

    def ige256_encrypt(data: bytes, key: bytes, iv: bytes) -> bytes:
        return bytes(ArcCrypto.ige256Encrypt(data, key, iv))

    def ctr256_decrypt(data: bytes, key: bytes, iv: bytearray, state: bytearray = None) -> bytes:
        s = state if state is not None else bytearray(1)
        res = bytes(ArcCrypto.ctr256(data, key, iv, s))
        return res

    def ctr256_encrypt(data: bytes, key: bytes, iv: bytearray, state: bytearray = None) -> bytes:
        s = state if state is not None else bytearray(1)
        res = bytes(ArcCrypto.ctr256(data, key, iv, s))
        return res

    def xor(a: bytes, b: bytes) -> bytes:
        return int.to_bytes(
            int.from_bytes(a, "big") ^ int.from_bytes(b, "big"),
            len(a),
            "big",
        )

except Exception as e:
    # Fallback to pyaes if running outside Android runtime
    import pyaes

    def xor(a: bytes, b: bytes) -> bytes:
        return int.to_bytes(
            int.from_bytes(a, "big") ^ int.from_bytes(b, "big"),
            len(a),
            "big",
        )

    def ige(data: bytes, key: bytes, iv: bytes, encrypt: bool) -> bytes:
        cipher = pyaes.AES(key)
        iv_1 = iv[:16]
        iv_2 = iv[16:]
        data_blocks = [data[i: i + 16] for i in range(0, len(data), 16)]
        if encrypt:
            for i, chunk in enumerate(data_blocks):
                iv_1 = data_blocks[i] = xor(cipher.encrypt(xor(chunk, iv_1)), iv_2)
                iv_2 = chunk
        else:
            for i, chunk in enumerate(data_blocks):
                iv_2 = data_blocks[i] = xor(cipher.decrypt(xor(chunk, iv_2)), iv_1)
                iv_1 = chunk
        return b"".join(data_blocks)

    def ige256_encrypt(data: bytes, key: bytes, iv: bytes) -> bytes:
        return ige(data, key, iv, True)

    def ige256_decrypt(data: bytes, key: bytes, iv: bytes) -> bytes:
        return ige(data, key, iv, False)

    def ctr(data: bytes, key: bytes, iv: bytearray, state: bytearray) -> bytes:
        cipher = pyaes.AES(key)
        out = bytearray(data)
        chunk = cipher.encrypt(iv)
        for i in range(0, len(data), 16):
            for j in range(0, min(len(data) - i, 16)):
                out[i + j] ^= chunk[state[0]]
                state[0] += 1
                if state[0] >= 16:
                    state[0] = 0
                if state[0] == 0:
                    for k in range(15, -1, -1):
                        try:
                            iv[k] += 1
                            break
                        except ValueError:
                            iv[k] = 0
                    chunk = cipher.encrypt(iv)
        return out

    def ctr256_encrypt(data: bytes, key: bytes, iv: bytearray, state: bytearray = None) -> bytes:
        return ctr(data, key, iv, state or bytearray(1))

    def ctr256_decrypt(data: bytes, key: bytes, iv: bytearray, state: bytearray = None) -> bytes:
        return ctr(data, key, iv, state or bytearray(1))
