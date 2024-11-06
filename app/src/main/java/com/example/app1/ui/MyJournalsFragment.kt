package com.example.app1.ui

import android.app.DatePickerDialog
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.SortOption
import com.example.app1.data.model.JournalEntry
import com.example.app1.ui.adapters.JournalAdapter
import com.example.app1.ui.adapters.SortOptionAdapter
import com.example.app1.utils.PreferencesHelper
import com.example.app1.viewmodel.JournalEntryViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class MyJournalsFragment : Fragment() {

    private val journalViewModel: JournalEntryViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter
    private var journalList: List<JournalEntry> = listOf()
    private var isAscendingOrder = true
    private var selectedSortOption: SortOption? = null
    private lateinit var fabSort: FloatingActionButton
    private lateinit var fabSortDirection: ImageView
    private val sortOptions = listOf(
        SortOption("Nombre", R.drawable.ic_sort_name),
        SortOption("Fecha", R.drawable.ic_sort_date),
        SortOption("Estado de Edición", R.drawable.ic_sort_edit),
        SortOption("Estado de Borrador", R.drawable.ic_sort_draft),
        SortOption("Estado de Ánimo", R.drawable.ic_sort_mood)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_journals, container, false)



        // Configura el RecyclerView y el Adaptador
        journalAdapter = JournalAdapter(listOf(), { draft -> journalViewModel.publishJournalEntry(draft) }, { journalId -> showJournalDetails(journalId) })
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = journalAdapter
        fabSort = view.findViewById(R.id.fabSort)
        fabSortDirection = view.findViewById(R.id.fabSortDirection)
        updateSortDirectionIcon(isAscendingOrder)
        // Listener para el FAB de ordenar
        view.findViewById<FloatingActionButton>(R.id.fabSort).setOnClickListener {
            showSortBottomSheet()
        }

        // Listener para el FAB de buscar
        view.findViewById<FloatingActionButton>(R.id.fabSearch).setOnClickListener {
            showSearchBottomSheet()
        }

        // Observa los journals del ViewModel y guarda la lista completa
        journalViewModel.getUserJournals(PreferencesHelper.getUserId(requireContext()) ?: "").observe(viewLifecycleOwner) { journals ->
            journalList = journals
            journalAdapter.updateJournals(journalList) // Actualiza el adaptador con la lista inicial
        }





        return view
    }

    private fun setupFilterListeners(view: View) {
        // Configura los botones de filtro para favoritos


        // Configura los botones de filtro para borradores
        view.findViewById<Button>(R.id.btnDraft).setOnClickListener {
            applyFilter { it.isDraft }
        }
        view.findViewById<Button>(R.id.btnNotDraft).setOnClickListener {
            applyFilter { !it.isDraft }
        }



        // Configura el filtro de búsqueda por nombre
        val nameFilterEditText = view.findViewById<EditText>(R.id.etNameFilter)
        nameFilterEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val query = nameFilterEditText.text.toString()
                applyFilter { it.title.contains(query, ignoreCase = true) }
                true
            } else {
                false
            }
        }

        // Configura el botón de filtro de fecha (abre un DatePickerDialog)
        view.findViewById<Button>(R.id.btnDateFilter).setOnClickListener {
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                // Crear una instancia de Calendar con la fecha seleccionada
                val selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0) // Ignora la hora para comparar solo la fecha
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                // Aplicar el filtro comparando la fecha ignorando la hora
                applyFilter { journalEntry ->
                    val journalDate = Calendar.getInstance().apply {
                        timeInMillis = journalEntry.date // Asumiendo que `journalEntry.date` es un Long en milisegundos
                        set(Calendar.HOUR_OF_DAY, 0) // Ignora la hora
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    journalDate == selectedDate // Comparación solo de la fecha (día, mes, año)
                }
            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

            datePicker.show()
        }

        // Configura los botones de filtro de estado de ánimo
        view.findViewById<Button>(R.id.btnHappyMood).setOnClickListener {
            applyFilter { it.mood == 5 } // Asume que 5 es "Feliz"
        }
        view.findViewById<Button>(R.id.btnSadMood).setOnClickListener {
            applyFilter { it.mood == 1 } // Asume que 1 es "Triste"
        }
        view.findViewById<Button>(R.id.btnAngryMood).setOnClickListener {
            applyFilter { it.mood == 2 } // Asume que 2 es "Ira"
        }
        view.findViewById<Button>(R.id.btnSurprisedMood).setOnClickListener {
            applyFilter { it.mood == 3 } // Asume que 3 es "Sorpresa"
        }
        view.findViewById<Button>(R.id.btnFearMood).setOnClickListener {
            applyFilter { it.mood == 4 } // Asume que 4 es "Miedo"
        }
        view.findViewById<Button>(R.id.btnDiscontentMood).setOnClickListener {
            applyFilter { it.mood == 6 } // Asume que 6 es "Inconforme"
        }

        // Configura el botón OK para aplicar todos los filtros actuales
        view.findViewById<Button>(R.id.btnFilterOk).setOnClickListener {
            // Opcionalmente, podrías combinar filtros aquí si deseas aplicar múltiples criterios
        }
    }


    private fun showSortBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_sort, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(bottomSheetView)

        val recyclerViewSortOptions = bottomSheetView.findViewById<RecyclerView>(R.id.recyclerViewSortOptions)
        recyclerViewSortOptions.layoutManager = LinearLayoutManager(requireContext())

        val adapter = SortOptionAdapter(sortOptions, selectedSortOption, isAscendingOrder) { option, ascending ->
            selectedSortOption = option
            isAscendingOrder = ascending
            applySort(option, ascending)
            updateSortButtonIcon(option)
            dialog.dismiss()
        }

        recyclerViewSortOptions.adapter = adapter
        dialog.show()
    }

    private fun showSearchBottomSheet() {
        // Infla el layout del bottom sheet para búsqueda
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_search, null)

        // Crea el BottomSheetDialog y establece el layout
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(bottomSheetView)

        // Configura los botones de filtro para borradores dentro del BottomSheet
        bottomSheetView.findViewById<Button>(R.id.btnDraft)?.setOnClickListener {
            applyFilter { it.isDraft }
            dialog.dismiss()
        }
        bottomSheetView.findViewById<Button>(R.id.btnNotDraft)?.setOnClickListener {
            applyFilter { !it.isDraft }
            dialog.dismiss()
        }

        // Configura el filtro de búsqueda por nombre dentro del BottomSheet
        val nameFilterEditText = bottomSheetView.findViewById<EditText>(R.id.etNameFilter)
        nameFilterEditText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val query = nameFilterEditText.text.toString()
                applyFilter { it.title.contains(query, ignoreCase = true) }
                dialog.dismiss()
                true
            } else {
                false
            }
        }

        // Configura el botón de filtro de fecha dentro del BottomSheet
        bottomSheetView.findViewById<Button>(R.id.btnDateFilter)?.setOnClickListener {
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                applyFilter { journalEntry ->
                    val journalDate = Calendar.getInstance().apply {
                        timeInMillis = journalEntry.date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    journalDate == selectedDate
                }
                dialog.dismiss()
            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

            datePicker.show()
        }

        // Configura los botones de filtro de estado de ánimo dentro del BottomSheet
        bottomSheetView.findViewById<Button>(R.id.btnHappyMood)?.setOnClickListener {
            applyFilter { it.mood == 5 } // Asume que 5 es "Feliz"
            dialog.dismiss()
        }
        bottomSheetView.findViewById<Button>(R.id.btnSadMood)?.setOnClickListener {
            applyFilter { it.mood == 1 } // Asume que 1 es "Triste"
            dialog.dismiss()
        }
        bottomSheetView.findViewById<Button>(R.id.btnAngryMood)?.setOnClickListener {
            applyFilter { it.mood == 2 } // Asume que 2 es "Ira"
            dialog.dismiss()
        }
        bottomSheetView.findViewById<Button>(R.id.btnSurprisedMood)?.setOnClickListener {
            applyFilter { it.mood == 3 } // Asume que 3 es "Sorpresa"
            dialog.dismiss()
        }
        bottomSheetView.findViewById<Button>(R.id.btnFearMood)?.setOnClickListener {
            applyFilter { it.mood == 4 } // Asume que 4 es "Miedo"
            dialog.dismiss()
        }
        bottomSheetView.findViewById<Button>(R.id.btnDiscontentMood)?.setOnClickListener {
            applyFilter { it.mood == 6 } // Asume que 6 es "Inconforme"
            dialog.dismiss()
        }

        // Muestra el diálogo
        dialog.show()
    }

    // Función para ordenar y actualizar el adaptador
    private fun <T : Comparable<T>> sortBy(selector: (JournalEntry) -> T?) {
        val sortedList = journalList.sortedBy(selector)
        journalAdapter.updateJournals(sortedList)
    }


    private fun applyFilter(predicate: (JournalEntry) -> Boolean) {
        val filteredList = journalList.filter(predicate)
        journalAdapter.updateJournals(filteredList)
    }

    fun toggleSortOrder() {
        isAscendingOrder = !isAscendingOrder // Cambia el orden
        updateSortDirectionIcon(isAscendingOrder) // Actualiza el ícono de dirección
    }

    private fun applySort(option: SortOption, isAscending: Boolean) {
        val sortedList = when (option.name) {
            "Nombre" -> journalList.sortedBy { it.title }
            "Fecha" -> journalList.sortedBy { it.date }
            "Estado de Edición" -> journalList.sortedBy { it.isEdited }
            "Estado de Borrador" -> journalList.sortedBy { it.isDraft }
            "Estado de Ánimo" -> journalList.sortedBy { it.mood }
            else -> journalList
        }
        journalAdapter.updateJournals(if (isAscending) sortedList else sortedList.reversed())
        updateSortDirectionIcon(isAscending)
    }

    private fun updateSortButtonIcon(option: SortOption) {
        // Actualiza solo el ícono de la opción seleccionada en fabSort
        fabSort.setImageResource(option.iconResId)
    }

    private fun updateSortDirectionIcon(isAscending: Boolean) {
        // Actualiza solo la flecha de dirección en fabSortDirection
        val arrowIcon = if (isAscending) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward
        fabSortDirection.setImageResource(arrowIcon)
    }

    private fun filterList(query: String) {
        val filteredList = journalList.filter { journal ->
            journal.title.contains(query, ignoreCase = true) ||
                    getMoodText(journal.mood).contains(query, ignoreCase = true)
        }
        journalAdapter.updateJournals(filteredList)
    }


    // Función para convertir el estado de ánimo a texto
    private fun getMoodText(mood: Int): String {
        return when (mood) {
            1 -> "Triste"
            2 -> "Ira"
            3 -> "Sorpresa"
            4 -> "Miedo"
            5 -> "Feliz"
            6 -> "Inconforme"
            else -> "Desconocido"
        }
    }

    private fun showJournalDetails(journalId: String) {
        JournalDetailFragment.newInstance(journalId).show(parentFragmentManager, "JournalDetail")
    }
}