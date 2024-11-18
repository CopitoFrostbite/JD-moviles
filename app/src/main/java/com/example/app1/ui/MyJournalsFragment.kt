package com.example.app1.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
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
import com.example.app1.utils.UiState
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
    private val activeFilters: MutableList<(JournalEntry) -> Boolean> = mutableListOf()
    private val tempFilters: MutableList<(JournalEntry) -> Boolean> = mutableListOf()
    private var searchBottomSheetDialog: BottomSheetDialog? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_journals, container, false)

// Configura el RecyclerView y el Adaptador
        journalAdapter = JournalAdapter(
            journals = listOf(),
            onPublishDraft = { draft -> journalViewModel.publishJournalEntry(draft) },
            onJournalClick = { journalId -> showJournalDetails(journalId) },
            onDelete = { journalId ->
                // Llama a markAsDeleted y pasa el ID directamente
                journalViewModel.markAsDeleted(journalId)

                // Observa el resultado de la eliminación
                journalViewModel.deleteStatus.observe(viewLifecycleOwner) { isDeleted ->
                    if (isDeleted) {
                        Toast.makeText(requireContext(), "Entrada eliminada con éxito", Toast.LENGTH_SHORT).show()
                        // Actualiza la lista eliminando el JournalEntry correspondiente
                        journalList = journalList.filter { it.journalId != journalId }
                        journalAdapter.updateJournals(journalList)
                    } else {
                        Toast.makeText(requireContext(), "Error al eliminar la entrada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
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

        // Botón de sincronización manual que verifica la conexión a internet antes de sincronizar todos los journals
        view.findViewById<FloatingActionButton>(R.id.fabSync).setOnClickListener {
            if (isConnectedToInternet()) {
                syncAllEntries()  // Llama a la función de sincronización manual en el ViewModel
                Toast.makeText(requireContext(), "Sincronización en proceso...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Conexión a internet no disponible", Toast.LENGTH_SHORT).show()
            }
        }
        // Observa el resultado de la sincronización
        journalViewModel.syncStatus.observe(viewLifecycleOwner) { result ->
            if (result) {
                Toast.makeText(requireContext(), "Sincronización exitosa", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Error en la sincronización", Toast.LENGTH_SHORT).show()
            }
        }

        //Observa los journals del ViewModel y guarda la lista completa
        journalViewModel.getUserJournals(PreferencesHelper.getUserId(requireContext()) ?: "")
            .observe(viewLifecycleOwner) { journals ->
                journalList = journals
                journalAdapter.updateJournals(journalList) // Asegúrate de actualizar el adaptador
            }


        return view
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
        searchBottomSheetDialog = BottomSheetDialog(requireContext())
        searchBottomSheetDialog?.setContentView(bottomSheetView)
        resetFilters(bottomSheetView)

        // Configura los listeners de filtro en el Bottom Sheet de búsqueda
        setupFilterListeners(bottomSheetView)

        // Solo mostrar teclado cuando se haga clic en el campo de búsqueda por nombre
        val nameFilterEditText = bottomSheetView.findViewById<EditText>(R.id.etNameFilter)
        nameFilterEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                nameFilterEditText.inputType = EditorInfo.TYPE_CLASS_TEXT
                nameFilterEditText.imeOptions = EditorInfo.IME_ACTION_DONE
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(nameFilterEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        nameFilterEditText?.clearFocus()
        setupFilterListeners(bottomSheetView)

        searchBottomSheetDialog?.show()
    }

    private fun setupFilterListeners(view: View) {
        val draftButtons = listOf(view.findViewById(R.id.btnDraft), view.findViewById<Button>(R.id.btnNotDraft))
        val moodButtons = listOf(
            view.findViewById(R.id.btnHappyMood),
            view.findViewById(R.id.btnSadMood),
            view.findViewById(R.id.btnAngryMood),
            view.findViewById(R.id.btnSurprisedMood),
            view.findViewById(R.id.btnFearMood),
            view.findViewById<Button>(R.id.btnDiscontentMood)
        )

        // Configura el filtro de borrador para que solo uno pueda ser seleccionado a la vez
        draftButtons.forEach { button ->
            button?.setOnClickListener {
                toggleExclusiveFilter({ it.isDraft == (button == view.findViewById(R.id.btnDraft)) }, button, "Filtro de borrador aplicado", tempFilters, draftButtons)
            }
        }

        // Configura el filtro de estado de ánimo para que solo uno pueda ser seleccionado a la vez
        moodButtons.forEachIndexed { index, button ->
            button?.setOnClickListener {
                toggleExclusiveFilter({ it.mood == index + 1 }, button, "Filtro de estado de ánimo aplicado", tempFilters, moodButtons)
            }
        }

        // Filtro por nombre con botón "Confirmar" en el teclado
        val nameFilterEditText = view.findViewById<EditText>(R.id.etNameFilter)
        nameFilterEditText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val query = nameFilterEditText.text.toString()
                toggleFilter({ it.title.contains(query, ignoreCase = true) }, nameFilterEditText, "Filtro por nombre activado", tempFilters)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Filtro de fecha
        view.findViewById<Button>(R.id.btnDateFilter)?.setOnClickListener {
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                toggleFilter(
                    { journalEntry ->
                        val journalDate = Calendar.getInstance().apply {
                            timeInMillis = journalEntry.date
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        journalDate == selectedDate
                    },
                    view.findViewById(R.id.btnDateFilter),
                    "Filtro de fecha activado",
                    tempFilters
                )
            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

            datePicker.show()
        }

        // Botón OK para aplicar filtros
        view.findViewById<Button>(R.id.btnFilterOk)?.setOnClickListener {
            applyCombinedFilters(tempFilters)
            hideKeyboard()
            searchBottomSheetDialog?.dismiss() // Cierra el Bottom Sheet
        }

        // Botón para limpiar filtros
        view.findViewById<Button>(R.id.btnClearFilters)?.setOnClickListener {
            tempFilters.clear()
            draftButtons.forEach { it?.alpha = 1.0f }
            moodButtons.forEach { it?.alpha = 1.0f }
            view.findViewById<Button>(R.id.btnDateFilter)?.alpha = 1.0f
            nameFilterEditText?.setText("")
            Toast.makeText(requireContext(), "Filtros eliminados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetFilters(view: View) {
        // Limpia todos los filtros temporales
        activeFilters.clear()
        tempFilters.clear()
    }

    private fun toggleFilter(
        filter: (JournalEntry) -> Boolean,
        button: View,
        activeMessage: String,
        tempFilters: MutableList<(JournalEntry) -> Boolean>
    ) {
        if (tempFilters.contains(filter)) {
            tempFilters.remove(filter)
            button.alpha = 1.0f // Reset visual feedback
        } else {
            tempFilters.add(filter)
            button.alpha = 0.5f // Visual feedback to indicate active filter
        }
        Toast.makeText(requireContext(), activeMessage, Toast.LENGTH_SHORT).show()
    }

    private fun toggleExclusiveFilter(
        filter: (JournalEntry) -> Boolean,
        button: View,
        activeMessage: String,
        tempFilters: MutableList<(JournalEntry) -> Boolean>,
        exclusiveGroup: List<Button?>
    ) {
        // Elimina cualquier filtro anterior del mismo grupo en `tempFilters`
        tempFilters.removeAll { existingFilter ->
            exclusiveGroup.any { it?.alpha == 0.5f }
        }

        // Restablece el color de todos los botones en el grupo exclusivo
        exclusiveGroup.forEach { btn ->
            btn?.alpha = 1.0f
        }

        // Activa el filtro seleccionado, aplicando feedback visual en el botón correspondiente
        toggleFilter(filter, button, activeMessage, tempFilters)
    }

    private fun applyCombinedFilters(filters: List<(JournalEntry) -> Boolean>) {
        activeFilters.clear()
        activeFilters.addAll(filters)

        if (activeFilters.isEmpty()) {
            journalAdapter.updateJournals(journalList) // Muestra la lista completa si no hay filtros
        } else {
            val filteredList = journalList.filter { journal ->
                activeFilters.all { filter -> filter(journal) } // Aplica todos los filtros
            }
            journalAdapter.updateJournals(filteredList)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
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


    // Función para convertir el estado de ánimo a texto


    private fun showJournalDetails(journalId: String) {
        JournalDetailFragment.newInstance(journalId).show(parentFragmentManager, "JournalDetail")
    }

    private fun syncAllEntries() {
        val userId = PreferencesHelper.getUserId(requireContext())
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Usuario no encontrado. Por favor, inicia sesión.", Toast.LENGTH_SHORT).show()
            return
        }
        journalViewModel.syncAllEntries(userId)
    }

    private fun isConnectedToInternet(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}