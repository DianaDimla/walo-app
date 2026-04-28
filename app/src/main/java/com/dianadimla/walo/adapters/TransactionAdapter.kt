/**
 * Adapter for displaying financial transactions in a list.
 * Categorises and formats currency values, applying visual distinction between income and expenses.
 */
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

class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder class to manage the display of a single transaction entry
    class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        // Binds transaction data and formats visual representation based on transaction type
        fun bind(transaction: Transaction) {
            binding.transactionCategoryText.text = transaction.podName ?: transaction.category
            binding.transactionNoteText.text = transaction.note ?: ""

            // Format monetary amount as Euro currency
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)
            val formattedAmount = currencyFormat.format(transaction.amount)

            if (transaction.expense) {
                // Apply red color and negative sign for expenses
                binding.transactionAmountText.text = "-${formattedAmount}"
                binding.transactionAmountText.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                )
            } else {
                // Apply green color and positive sign for income
                binding.transactionAmountText.text = "+${formattedAmount}"
                binding.transactionAmountText.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
                )
            }
        }
    }

    // Handles optimised list updates using unique transaction IDs
    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
