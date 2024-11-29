package com.movtery.zalithlauncher.ui.subassembly.account

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.ViewAccountBinding
import com.movtery.zalithlauncher.feature.accounts.AccountUtils
import com.movtery.zalithlauncher.feature.accounts.AccountsManager
import com.movtery.zalithlauncher.ui.fragment.AccountFragment
import com.movtery.zalithlauncher.ui.fragment.FragmentWithAnim
import com.movtery.zalithlauncher.utils.ZHTools
import com.movtery.zalithlauncher.utils.skin.SkinLoader
import net.kdt.pojavlaunch.Tools

class AccountViewWrapper(private val parentFragment: FragmentWithAnim? = null, val binding: ViewAccountBinding) {
    private val mContext: Context = binding.root.context

    init {
        parentFragment?.let { fragment ->
            binding.root.setOnClickListener {
                ZHTools.swapFragmentWithAnim(fragment, AccountFragment::class.java, AccountFragment.TAG, null)
            }
        }
    }

    fun refreshAccountInfo() {
        binding.apply {
            val account = AccountsManager.getInstance().currentAccount
            account ?: run {
                if (parentFragment == null) {
                    userIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_help))
                    userName.text = null
                } else {
                    userIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_add))
                    userName.setText(R.string.account_add)
                }
                accountType.visibility = View.GONE
                return
            }
            userIcon.setImageDrawable(SkinLoader.getAvatarDrawable(mContext, account, Tools.dpToPx(52f).toInt()))
            userName.text = account.username
            accountType.text = AccountUtils.getAccountTypeName(mContext, account)
            accountType.visibility = View.VISIBLE
        }
    }
}
