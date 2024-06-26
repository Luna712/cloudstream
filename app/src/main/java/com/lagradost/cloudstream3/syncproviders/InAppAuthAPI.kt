package com.lagradost.cloudstream3.syncproviders

interface InAppAuthAPI : AuthAPI {
    data class UserData(
        val username: String? = null,
        val password: String? = null,
        val server: String? = null,
        val email: String? = null,
    )

    // this is for displaying the UI
    val requiresPassword: Boolean
    val requiresUsername: Boolean
    val requiresServer: Boolean
    val requiresEmail: Boolean

    // if this is false we can assume that getLatestLoginData returns null and wont be called
    // this is used in case for some reason it is not preferred to store any login data besides the "token" or encrypted data
    val storesPasswordInPlainText: Boolean

    // return true if logged in successfully
    suspend fun login(data: UserData): Boolean

    // used to fill the UI if you want to edit any data about your login info
    fun getUserData(): UserData?
}

abstract class InAppAuthAPIManager(defIndex: Int) : AccountManager(defIndex), InAppAuthAPI {
    override val requiresPassword = false
    override val requiresUsername = false
    override val requiresEmail = false
    override val requiresServer = false
    override val storesPasswordInPlainText = true
    override val requiresLogin = true

    override fun logOut() {
        throw NotImplementedError()
    }

    override val idPrefix: String
        get() = throw NotImplementedError()

    override val name: String
        get() = throw NotImplementedError()

    override val icon: Int? = null

    override suspend fun login(data: InAppAuthAPI.UserData): Boolean {
        throw NotImplementedError()
    }

    override fun getUserData(): InAppAuthAPI.UserData? {
        throw NotImplementedError()
    }

    override fun loginInfo(): AuthAPI.LoginInfo? {
        throw NotImplementedError()
    }
}