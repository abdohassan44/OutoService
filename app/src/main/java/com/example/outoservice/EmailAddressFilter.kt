package com.example.outoservice

import android.app.PendingIntent
import android.app.assist.AssistStructure.ViewNode
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.egorshustov.autofilltest.EMAIL_PREFERENCES
import com.egorshustov.autofilltest.EMAIL_PRIMARY_PREF
import com.egorshustov.autofilltest.EMAIL_SECONDARY_PREF


class EmailAddressFilter : AutofillService() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts[request.fillContexts.size - 1].structure

        val foundEmailFields: MutableList<ViewNode?> = ArrayList()
        identifyEmailFields(structure.getWindowNodeAt(0).rootViewNode, foundEmailFields)
        if (foundEmailFields.size == 0) return

        val sharedPreferences = getSharedPreferences(EMAIL_PREFERENCES, Context.MODE_PRIVATE)
        val primaryEmailText = sharedPreferences.getString(EMAIL_PRIMARY_PREF, "")
        val secondaryEmailText = sharedPreferences.getString(EMAIL_SECONDARY_PREF, "")

        val primaryEmailView = RemoteViews(packageName, R.layout.item_email_suggestion)
        primaryEmailView.setTextViewText(R.id.text_email_suggestion, primaryEmailText)
        primaryEmailView.setOnClickResponse(
            R.id.button_some_action, RemoteViews.RemoteResponse.fromPendingIntent(
                PendingIntent.getActivity(
                    this,
                    1001,
                    Intent(this, SecondActivity::class.java),
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
        )

        val secondaryEmailView = RemoteViews(packageName, R.layout.item_email_suggestion)
        secondaryEmailView.setTextViewText(R.id.text_email_suggestion, secondaryEmailText)

        val responseBuilder = FillResponse.Builder()

        foundEmailFields.forEach {
            val primaryEmailDataSet = Dataset.Builder(primaryEmailView).setValue(
                it?.autofillId!!,
                AutofillValue.forText(primaryEmailText)
            ).build()

            responseBuilder.addDataset(primaryEmailDataSet)

            val secondaryEmailDataSet = Dataset.Builder(secondaryEmailView).setValue(
                it.autofillId!!,
                AutofillValue.forText(secondaryEmailText)
            ).build()

            responseBuilder.addDataset(secondaryEmailDataSet)
        }

        callback.onSuccess(responseBuilder.build())
    }

    private fun identifyEmailFields(node: ViewNode, emailFields: MutableList<ViewNode?>) {
        if (node.className?.contains("EditText") == true) {
            val viewId = node.idEntry
            if (viewId != null && (viewId.contains("email") || viewId.contains("username"))) {
                emailFields.add(node)
                return
            }
        }

        for (i in 0 until node.childCount) identifyEmailFields(node.getChildAt(i), emailFields)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
    }

    override fun onConnected() {
        super.onConnected()
        Log.d(TAG, "onConnected")
    }

    override fun onDisconnected() {
        super.onDisconnected()
        Log.d(TAG, "onDisconnected")
    }

    companion object {
        private const val TAG = "EmailAddressFiller"
    }

}