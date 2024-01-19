package com.example.anycall

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.example.anycall.databinding.FragmentMyPageBinding
import com.example.anycall.databinding.MyPageDialogBinding

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class MyPageFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private val binding by lazy { FragmentMyPageBinding.inflate(layoutInflater) }
    private val favoriteAdapter by lazy { FavoriteAdapter(binding.itemFavoriteEmptyText) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initInfo()
        initViewpager()
        initViewpagerButton()
        initEditButton()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewPagerVisible()
        favoriteAdapter.submitList(MyItem.dataList.filter { it.favorite }.toMutableList())
    }

    private fun initInfo() {
        with(binding) {
            mypageProfileName.text = User.getUser().name
            val formattedPhoneNum = StringBuilder(User.getUser().phoneNum)
                .insert(3, "-")
                .insert(8, "-")
                .toString()
            mypagePhone.text = formattedPhoneNum
            mypageMessage.text = User.getUser().myMessage
            mypageEmail.text = User.getUser().email
        }
    }

    private fun initViewpager() {
        favoriteAdapter.apply {
            submitList(MyItem.dataList.filter { it.favorite }.toMutableList())
            listener = object : FavoriteAdapter.OnItemClickListener {
                override fun onPhoneClick(data: MyItem, pos: Int) {
                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${data.phoneNum}"))
                    startActivity(intent)
                }

                override fun onItemClick(data: MyItem) {
                    requireActivity().supportFragmentManager.beginTransaction().apply {
                        replace(R.id.frame, ContactDetailFragment.newInstance(data) )
                        setReorderingAllowed(true)
                        addToBackStack("")
                    }.commit()
                }
            }
        }

        with(binding) {
            mypageViewpager.apply {
                adapter = favoriteAdapter
                isUserInputEnabled = false
            }
            binding.mypageViewpagerIndicator.attachTo(binding.mypageViewpager)
        }

    }

    private fun viewPagerVisible() {
        if (MyItem.dataList.filter { it.favorite }.toMutableList().size == 0) {
            with(binding) {
                icArrowBack.visibility = View.GONE
                icArrowForward.visibility = View.GONE
                mypageViewpagerIndicator.visibility = View.GONE
            }
        } else {
            with(binding) {
                icArrowBack.visibility = View.VISIBLE
                icArrowForward.visibility = View.VISIBLE
                mypageViewpagerIndicator.visibility = View.VISIBLE
            }
        }
    }

    private fun initViewpagerButton() {
        viewPagerVisible()
        with(binding) {
            icArrowBack.setOnClickListener {
                val current = mypageViewpager.currentItem

                mypageViewpager.setCurrentItem(current - 1, true)
                if (current == 0) {
                    binding.mypageViewpager.setCurrentItem(favoriteAdapter.itemCount - 1, true)
                }
            }

            icArrowForward.setOnClickListener {
                val current = mypageViewpager.currentItem

                mypageViewpager.setCurrentItem(current + 1, true)
                if (current == favoriteAdapter.itemCount - 1) {
                    binding.mypageViewpager.setCurrentItem(0, true)
                }
            }
        }
    }

    private fun initEditButton() {
        binding.mypageEdit.setOnClickListener {
            makeDialog()
        }
    }

    private fun makeDialog() {
        val dialogView = MyPageDialogBinding.inflate(layoutInflater)

        val alertDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView.root)
            .setCancelable(false)
            .create()

        dialogView.mypageDialogButton.setOnClickListener {
            val message = dialogView.mypageDialogEdit.text.toString()
            binding.mypageMessage.text = message
            User.updateUserMessage(message)
            alertDialog.dismiss()
        }

        dialogView.mypageDialogCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

}