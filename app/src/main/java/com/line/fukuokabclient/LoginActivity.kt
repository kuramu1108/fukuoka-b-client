package com.line.fukuokabclient

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.GsonBuilder
import com.line.fukuokabclient.Utility.Prefs
import com.line.fukuokabclient.client.UserClient
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {
    var mAuth: FirebaseAuth? = null
    var mUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        //user = User(id = 1, clientNumber = "PT445")

        checkLoggedIn()

        btn_login.setOnClickListener {
            mAuth!!.signInWithEmailAndPassword(txt_email.text.toString(), txt_password.text.toString())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Prefs.edit(applicationContext)
                                    .putString("email", txt_email.text.toString())
                                    .putString("password", txt_password.text.toString())
                                    .apply()
                            Toast.makeText(applicationContext, "Signed in", Toast.LENGTH_LONG).show()
                            mUser = mAuth!!.currentUser
                            updateUI(mUser!!)
                        } else {
                            Toast.makeText(applicationContext, "Logged in failed", Toast.LENGTH_LONG).show()
                        }

                    }

            //Toast.makeText(applicationContext, "Submitted", Toast.LENGTH_LONG).show()
        }
    }

    fun updateUI(mUser:FirebaseUser) {
        val gson = GsonBuilder()
                .create()

        val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASEURL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()

        val userClient = retrofit.create(UserClient::class.java)

        userClient.getUserByMail(mUser.email!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Prefs.edit(applicationContext)
                            .putLong("id", it.id)
                            .apply()

                    var intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                }, {
                    Log.d("LOGIN", "LOGIN")
                })

    }

    fun checkLoggedIn() {
        txt_email.setText(Prefs.get(applicationContext).getString("email", ""))
        txt_password.setText(Prefs.get(applicationContext).getString("password", ""))
    }
}
