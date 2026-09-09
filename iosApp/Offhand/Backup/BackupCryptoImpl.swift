import CommonCrypto
import CryptoKit
import Foundation
import OffhandShared
import Security

final class BackupCryptoImpl: IosBackupCryptoBridge {
    func randomBytes(count: Int32) -> Data {
        var bytes = [UInt8](repeating: 0, count: Int(count))
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        precondition(status == errSecSuccess, "SecRandomCopyBytes failed with \(status)")
        return Data(bytes)
    }

    func deriveKey(passphrase: Data, salt: Data, iterations: Int32, keyLength: Int32) -> Data {
        var derived = [UInt8](repeating: 0, count: Int(keyLength))
        let status = passphrase.withUnsafeBytes { passphraseBytes in
            salt.withUnsafeBytes { saltBytes in
                CCKeyDerivationPBKDF(
                    CCPBKDFAlgorithm(kCCPBKDF2),
                    passphraseBytes.bindMemory(to: Int8.self).baseAddress,
                    passphrase.count,
                    saltBytes.bindMemory(to: UInt8.self).baseAddress,
                    salt.count,
                    CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                    UInt32(iterations),
                    &derived,
                    derived.count
                )
            }
        }
        precondition(status == kCCSuccess, "PBKDF2 failed with \(status)")
        return Data(derived)
    }

    func seal(key: Data, nonce: Data, associatedData: Data, plaintext: Data) -> Data {
        do {
            let box = try AES.GCM.seal(
                plaintext,
                using: SymmetricKey(data: key),
                nonce: AES.GCM.Nonce(data: nonce),
                authenticating: associatedData
            )
            return box.ciphertext + box.tag
        } catch {
            preconditionFailure("AES-GCM seal failed: \(error)")
        }
    }

    func open(key: Data, nonce: Data, associatedData: Data, ciphertext: Data) -> Data? {
        guard ciphertext.count >= Self.tagLength else { return nil }
        let tagStart = ciphertext.count - Self.tagLength
        guard let box = try? AES.GCM.SealedBox(
            nonce: AES.GCM.Nonce(data: nonce),
            ciphertext: ciphertext.prefix(tagStart),
            tag: ciphertext.suffix(from: tagStart)
        ) else { return nil }
        return try? AES.GCM.open(box, using: SymmetricKey(data: key), authenticating: associatedData)
    }

    private static let tagLength = 16
}
