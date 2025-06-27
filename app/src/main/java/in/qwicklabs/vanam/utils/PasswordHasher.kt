package `in`.qwicklabs.vanam.utils

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    private const val BCRYPT_COST = 12

    fun hashPassword(plainPassword: String): String {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST))
    }

    fun checkPassword(plainPassword: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(plainPassword, hashedPassword)
    }
}