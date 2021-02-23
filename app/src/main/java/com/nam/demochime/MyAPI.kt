package com.nam.demochime

import io.reactivex.Observable
import retrofit2.http.POST
import retrofit2.http.Query

interface MyAPI {
    @POST("join")
    fun checkMeeting(@Query("title") id: String,@Query("name") name: String,@Query("region") region: String)
            : Observable<JoinMeetingResponse>
}