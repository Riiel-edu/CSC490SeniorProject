package com.example.csc490seniorproject.objects
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity
class User (
    @PrimaryKey var username: String,
    @ColumnInfo var first_name: String,
    @ColumnInfo var last_name: String,
    @ColumnInfo var privacy: Boolean,
    @ColumnInfo var DoB: String,
    @ColumnInfo var email: String,
    @ColumnInfo var songs: List<Song>,
    @ColumnInfo var friends: List<User>
    ){
}