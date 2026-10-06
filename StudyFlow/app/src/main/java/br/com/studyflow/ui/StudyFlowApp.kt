package br.com.studyflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import br.com.studyflow.Priority
import br.com.studyflow.data.model.Avaliacao
import br.com.studyflow.data.model.Materia
import java.text.SimpleDateFormat
import java.util.*

private enum class StatusFilter(val label: String) { TODAS("Todas"), PENDENTES("Pendentes"), CONCLUIDAS("Concluídas") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyFlowApp(vm: StudyViewModel) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF3659C9), secondary = Color(0xFF52658F))) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("StudyFlow", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                )
            },
            bottomBar = {
                NavigationBar {
                    listOf(
                        Triple("home", "Início", Icons.Default.Home),
                        Triple("materias", "Matérias", Icons.Default.Book),
                        Triple("avaliacoes", "Avaliações", Icons.Default.Event)
                    ).forEach { (target, label, icon) ->
                        NavigationBarItem(
                            selected = route == target,
                            onClick = {
                                nav.navigate(target) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(nav, "home", Modifier.padding(padding)) {
                composable("home") { HomeScreen(vm) }
                composable("materias") { MateriasScreen(vm) }
                composable("avaliacoes") { AvaliacoesScreen(vm) }
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: StudyViewModel) {
    val pending by vm.prioridades.collectAsState()
    val all by vm.avaliacoes.collectAsState()
    val materias by vm.materias.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Visão geral", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Organize o que precisa de atenção primeiro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("Pendentes", pending.size.toString(), Modifier.weight(1f))
                SummaryCard("Concluídas", all.count { it.concluida }.toString(), Modifier.weight(1f))
                SummaryCard("Matérias", materias.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Text("Prioridade de estudos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Cálculo: 60% urgência + 40% impacto", style = MaterialTheme.typography.bodySmall)
        }
        if (pending.isEmpty()) item { EmptyState("Tudo em dia!", "Cadastre uma avaliação para começar.") }
        items(pending, key = { it.id }) { evaluation ->
            EvaluationCard(evaluation, materias.firstOrNull { it.id == evaluation.materiaId }, true, vm)
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MateriasScreen(vm: StudyViewModel) {
    val materias by vm.materias.collectAsState()
    val avaliacoes by vm.avaliacoes.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Materia?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Matérias", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${materias.size} cadastrada(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (materias.isEmpty()) item { EmptyState("Nenhuma matéria", "Use o botão + para cadastrar a primeira.") }
            items(materias, key = { it.id }) { materia ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    ListItem(
                        leadingContent = { Icon(Icons.Default.Book, null) },
                        headlineContent = { Text(materia.nome, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(listOfNotNull(materia.professor.takeIf { it.isNotBlank() }, "${avaliacoes.count { it.materiaId == materia.id }} avaliação(ões)").joinToString(" • ")) },
                        trailingContent = { IconButton(onClick = { deleteTarget = materia }) { Icon(Icons.Default.Delete, "Excluir matéria") } }
                    )
                }
            }
        }
        FloatingActionButton(onClick = { showForm = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Adicionar matéria")
        }
    }
    if (showForm) MatterDialog(onDismiss = { showForm = false }) { name, professor -> vm.addMateria(name, professor); showForm = false }
    deleteTarget?.let { materia ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null }, icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Excluir ${materia.nome}?") },
            text = { Text("As avaliações vinculadas a esta matéria também serão excluídas.") },
            confirmButton = { TextButton(onClick = { vm.deleteMateria(materia.id); deleteTarget = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MatterDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var professor by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Nova matéria") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome *") }, singleLine = true)
            OutlinedTextField(professor, { professor = it }, label = { Text("Professor (opcional)") }, singleLine = true)
        } },
        confirmButton = { Button(onClick = { onSave(name.trim(), professor.trim()) }, enabled = name.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvaliacoesScreen(vm: StudyViewModel) {
    val materias by vm.materias.collectAsState()
    val avaliacoes by vm.avaliacoes.collectAsState()
    var status by rememberSaveable { mutableStateOf(StatusFilter.TODAS) }
    var selectedMatter by rememberSaveable { mutableStateOf<Long?>(null) }
    var showForm by remember { mutableStateOf(false) }
    val filtered = avaliacoes.filter {
        (selectedMatter == null || it.materiaId == selectedMatter) && when (status) {
            StatusFilter.TODAS -> true
            StatusFilter.PENDENTES -> !it.concluida
            StatusFilter.CONCLUIDAS -> it.concluida
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Avaliações", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Filtre e acompanhe todos os prazos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusFilter.entries.forEach { item -> FilterChip(status == item, { status = item }, label = { Text(item.label) }) }
                }
                MatterFilter(materias, selectedMatter) { selectedMatter = it }
            }
            if (filtered.isEmpty()) item { EmptyState("Nenhum resultado", "Altere os filtros ou adicione uma avaliação.") }
            items(filtered, key = { it.id }) { evaluation -> EvaluationCard(evaluation, materias.firstOrNull { it.id == evaluation.materiaId }, false, vm) }
        }
        ExtendedFloatingActionButton(
            onClick = { showForm = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Nova avaliação") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp), containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
    if (showForm) EvaluationDialog(materias, onDismiss = { showForm = false }) { vm.addAvaliacao(it); showForm = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatterFilter(materias: List<Materia>, selected: Long?, onSelected: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = materias.firstOrNull { it.id == selected }?.nome ?: "Todas as matérias", onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Matéria") }
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem({ Text("Todas as matérias") }, onClick = { onSelected(null); expanded = false })
            materias.forEach { m -> DropdownMenuItem({ Text(m.nome) }, onClick = { onSelected(m.id); expanded = false }) }
        }
    }
}

@Composable
private fun EvaluationCard(evaluation: Avaliacao, materia: Materia?, compact: Boolean, vm: StudyViewModel) {
    val score = Priority.score(evaluation)
    val priorityLabel = when { score >= 80 -> "Alta"; score >= 55 -> "Média"; else -> "Baixa" }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(evaluation.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${materia?.nome ?: "Sem matéria"} • ${evaluation.tipo}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = {}, label = { Text("$priorityLabel · $score") }, enabled = false)
            }
            Text("Prazo: ${formatDate(evaluation.dataMillis)}  •  Peso: ${formatWeight(evaluation.peso)}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!compact) IconButton(onClick = { vm.delete(evaluation) }) { Icon(Icons.Default.Delete, "Excluir avaliação") }
                TextButton(onClick = { vm.toggle(evaluation) }) {
                    Icon(if (evaluation.concluida) Icons.Default.Refresh else Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(6.dp)); Text(if (evaluation.concluida) "Reabrir" else "Concluir")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EvaluationDialog(materias: List<Materia>, onDismiss: () -> Unit, onSave: (Avaliacao) -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Prova") }
    var weight by remember { mutableStateOf("5") }
    var materiaId by remember { mutableStateOf(materias.firstOrNull()?.id) }
    var showDatePicker by remember { mutableStateOf(false) }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 3 * 86_400_000L) }
    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    val valid = title.isNotBlank() && materiaId != null && weightValue != null && weightValue in 0.0..5.0
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Nova avaliação") },
        text = { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (materias.isEmpty()) Text("Cadastre uma matéria antes de criar avaliações.", color = MaterialTheme.colorScheme.error)
            OutlinedTextField(title, { title = it }, label = { Text("Título *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Prova", "Trabalho", "Seminário").forEach { option -> FilterChip(type == option, { type = option }, label = { Text(option) }) }
            }
            MatterFilter(materias, materiaId) { materiaId = it }
            OutlinedTextField(
                weight, { weight = it }, label = { Text("Peso de 0 a 5 *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, isError = weight.isNotBlank() && (weightValue == null || weightValue !in 0.0..5.0), modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { showDatePicker = true }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(8.dp)); Text("Prazo: ${formatDate(dateMillis)}")
            }
            Text("Lembretes serão programados para 7, 3 e 1 dia antes.", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { Button(onClick = {
            onSave(Avaliacao(titulo = title.trim(), tipo = type, materiaId = materiaId!!, dataMillis = dateMillis, peso = weightValue!!))
        }, enabled = valid) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { dateMillis = utcDateToLocalMidnight(it) }; showDatePicker = false }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EventNote, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDate(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(millis))
private fun formatWeight(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
private fun utcDateToLocalMidnight(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
