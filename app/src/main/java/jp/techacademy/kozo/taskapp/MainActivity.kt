package jp.techacademy.kozo.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

const val EXTRA_TASK = "jp.techacademy.kozo.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }
    private lateinit var mTaskAdapter: TaskAdapter


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //val coordinatorLayout = findViewById<CoordinatorLayout>(R.id.coordinator_layout)
        val focusView = currentFocus ?: return false
        inputMethodManager.hideSoftInputFromWindow(
            focusView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )

        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this)
        reloadListView()


        // category検索
        category_search_button.setOnClickListener { view ->
            // close a keyboard
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // search category by text
            val searchText = category_search_text_box.text.toString()
            if (searchText.isNotEmpty()){
                reloadListView(searchText)
            } else {
                Snackbar.make(view, "検索したいカテゴリ名を入力してください。", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }


        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            val task = parent.adapter.getItem(position) as Task
            val builder = AlertDialog.Builder(this)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }
            builder.setNegativeButton("CANCEL", null)
            val dialog = builder.create()
            dialog.show()
            true
        }

        //addTaskForTest()
        reloadListView()
    }

    private fun reloadListView(categorySearchText: String="") {
        val taskRealmResults: RealmResults<Task> = if (categorySearchText.isEmpty()) {
            mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
        }else {
            mRealm.where(Task::class.java)
                .beginsWith("category", categorySearchText)
                .findAll().sort("date", Sort.DESCENDING)
        }
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)
        listView1.adapter = mTaskAdapter
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

    private fun addTaskForTest() {
        Log.d("TaskApp", "addTest")

        val task = Task()
        task.title = "作業"
        task.contents = "プログラムを書いてPUSHする"
        task.date = Date()
        task.id = 0
        mRealm.beginTransaction()
        mRealm.copyToRealmOrUpdate(task)
        mRealm.commitTransaction()
    }

}