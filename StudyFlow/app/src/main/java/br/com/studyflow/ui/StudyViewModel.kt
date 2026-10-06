package br.com.studyflow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.studyflow.Priority
import br.com.studyflow.data.db.AppDatabase
import br.com.studyflow.data.model.Avaliacao
import br.com.studyflow.data.repository.StudyRepository
import br.com.studyflow.notifications.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudyViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val repo = StudyRepository(db.materiaDao(), db.avaliacaoDao())

    val materias = repo.materias.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val avaliacoes = repo.avaliacoes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val prioridades = avaliacoes.map { list ->
        list.filter { !it.concluida }.sortedByDescending { Priority.score(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addMateria(nome: String, professor: String) = viewModelScope.launch {
        if (nome.isNotBlank()) repo.inserirMateria(nome.trim(), professor.trim())
    }
    fun deleteMateria(id: Long) = viewModelScope.launch {
        avaliacoes.value.filter { it.materiaId == id }.forEach {
            ReminderScheduler.cancel(getApplication(), it.id)
        }
        materias.value.firstOrNull { it.id == id }?.let { repo.excluirMateria(it) }
    }
    fun addAvaliacao(a: Avaliacao) = viewModelScope.launch {
        val id = repo.inserirAvaliacao(a)
        ReminderScheduler.schedule(getApplication(), a.copy(id = id))
    }
    fun toggle(a: Avaliacao) = viewModelScope.launch {
        repo.concluir(a)
        if (a.concluida) ReminderScheduler.schedule(getApplication(), a)
        else ReminderScheduler.cancel(getApplication(), a.id)
    }
    fun delete(a: Avaliacao) = viewModelScope.launch {
        ReminderScheduler.cancel(getApplication(), a.id)
        repo.excluirAvaliacao(a)
    }
}
