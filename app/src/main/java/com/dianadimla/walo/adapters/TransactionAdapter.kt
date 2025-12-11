package com.dianadimla.walo.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dianadimla.walo.R
import com.dianadimla.walo.data.Transaction
import com.dianadimla.walo.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.util.Locale

// Adapter for the transaction list.
class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    // Creates new views.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    // Binds data to views.
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Holds the views for a transaction item.
    class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        // Binds transaction data.
        fun bind(transaction: Transaction) {
            binding.transactionCategoryText.text = transaction.podName ?: transaction.category
            binding.transactionNoteText.text = transaction.note ?: ""

            // Format currency for Germany.
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)
            val formattedAmount = currencyFormat.format(transaction.amount)

            if (transaction.expense) {
                // Set text red for expense.
                binding.transactionAmountText.text = "-${formattedAmount}"
                binding.transactionAmountText.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                )
            } else {
                // Set text green for income.
                binding.transactionAmountText.text = "+${formattedAmount}"
                binding.transactionAmountText.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
                )
            }
        }
    }

    // DiffUtil for list updates.
    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        // Check if items are the same.
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        // Check if contents are the same.
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
