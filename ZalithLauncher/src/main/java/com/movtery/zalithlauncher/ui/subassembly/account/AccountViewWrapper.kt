package com.movtery.zalithlauncher.ui.subassembly.account

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.feature.accounts.AccountsManager
import com.movtery.zalithlauncher.ui.fragment.AccountFragment
import com.movtery.zalithlauncher.ui.fragment.FragmentWithAnim
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.skin.SkinLoader
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.value.MinecraftAccount

class AccountViewWrapper(private val parentFragment: FragmentWithAnim? = null, val mainView: View) {
    private val mContext: Context = mainView.context
    private val mUserIconView: ImageView = mainView.findViewById(R.id.user_icon)
    private val mUserNameView: TextView = mainView.findViewById(R.id.user_name)

    init {
        parentFragment?.let { fragment ->
            mainView.setOnClickListener {
                ZHTools.swapFragmentWithAnim(fragment, AccountFragment::class.java, AccountFragment.TAG, null)
            }
        }
    }

    fun refreshAccountInfo() {
        val account = currentAccount
        account ?: run {
            if (parentFragment == null) {
                mUserIconView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_help))
                mUserNameView.text = null
            } else {
                mUserIconView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_add))
                mUserNameView.setText(R.string.account_add)
            }
            return
        }
        mUserIconView.setImageDrawable(SkinLoader.getAvatarDrawable(mainView.context, account, Tools.dpToPx(52f).toInt()))
        mUserNameView.text = account.username
    }

    private val currentAccount: MinecraftAccount?
        get() = AccountsManager.getInstance().currentAccount
}
