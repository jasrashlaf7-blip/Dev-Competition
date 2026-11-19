package com.olympiades.firststep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.olympiades.firststep.data.local.entities.FavoriteEntity
import com.olympiades.firststep.data.local.entities.FavoriteWithOffer
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Transaction
    @Query("SELECT * FROM favorites ORDER BY added_at DESC, id DESC")
    fun observeAllFavorites(): Flow<List<FavoriteWithOffer>>

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeFavoriteCount(): Flow<Int>

    @Query("SELECT * FROM favorites WHERE offer_id = :offerId LIMIT 1")
    suspend fun getFavoriteByOfferId(offerId: Long): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Query("UPDATE favorites SET personal_note = :personalNote WHERE offer_id = :offerId")
    suspend fun updateFavoriteNote(offerId: Long, personalNote: String)

    @Query("DELETE FROM favorites WHERE offer_id = :offerId")
    suspend fun deleteFavoriteByOfferId(offerId: Long)
}
