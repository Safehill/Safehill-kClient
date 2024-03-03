package com.safehill.snoog.core.datastore.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "public_key", typeAffinity = ColumnInfo.BLOB) val publicKey: ByteArray,
    @ColumnInfo(name = "public_signature", typeAffinity = ColumnInfo.BLOB) val publicSignature: ByteArray,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!publicSignature.contentEquals(other.publicSignature)) return false
        if (name != other.name) return false
        return phoneNumber == other.phoneNumber
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + publicSignature.contentHashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (phoneNumber?.hashCode() ?: 0)
        return result
    }
}