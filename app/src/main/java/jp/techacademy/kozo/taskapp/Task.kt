package jp.techacademy.kozo.taskapp

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.util.*

open class Task: RealmObject(), Serializable{
    var title: String = ""
    var contents: String = ""
    var date: Date = Date()

    @PrimaryKey
    var id: Int = 0


}