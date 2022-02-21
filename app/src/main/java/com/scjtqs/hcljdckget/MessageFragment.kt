package com.scjtqs.hcljdckget

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.annotation.Nullable


class MessageDialog : DialogFragment() {
    private var title = "title"
    private var message = "message"

    fun setTitleMessage(title: String, message: String) {
        this.title = title
        this.message = message
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //创建对话框,我们需要返回dialog
        val dialog: AlertDialog.Builder = AlertDialog.Builder(context)
        dialog.setTitle(title)
        dialog.setMessage(message)
        return dialog.create()
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //此方法在视图已经创建后返回的，但是这个view 还没有添加到父级中，我们在这里可以重新设定view的各个数据
    }
}