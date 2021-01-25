package com.example.videomusic.ui.createpl

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.DialogFragment
import com.example.videomusic.databinding.FragmentCreatePlayListBinding

class RenamePLFragment : DialogFragment() {

    private lateinit var viewBinding: FragmentCreatePlayListBinding

    private var listener: OnPLRename? = null

    private var title = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        viewBinding = FragmentCreatePlayListBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        handleEvent()
    }

    private fun setupViews() = with(viewBinding) {
        buttonCreate.isEnabled = false
        textView.text = "Rename playlist: $title"
        buttonCreate.text = "Apply"
        editTitle.setText(title)
        editTitle.requestFocus()
    }

    private fun handleEvent() = with(viewBinding) {
        buttonCreate.setOnClickListener {
            dialog?.cancel()
            listener?.onPLRename(editTitle.text.toString())
        }
        editTitle.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                buttonCreate.isEnabled = !s.isNullOrBlank()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
        })
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    interface OnPLRename {
        fun onPLRename(title: String)
    }

    companion object {
        fun newInstance(listener: OnPLRename, title: String) = RenamePLFragment().apply {
            this.listener = listener
            this.title = title
        }
    }
}