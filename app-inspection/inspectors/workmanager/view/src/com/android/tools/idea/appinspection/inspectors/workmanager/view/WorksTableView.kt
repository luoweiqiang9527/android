package com.android.tools.idea.appinspection.inspectors.workmanager.view

import androidx.work.inspection.WorkManagerInspectorProtocol.Data
import androidx.work.inspection.WorkManagerInspectorProtocol.WorkInfo
import com.android.tools.idea.appinspection.inspectors.workmanager.model.WorkManagerInspectorClient
import com.android.tools.idea.appinspection.inspectors.workmanager.model.WorksTableModel
import com.google.wireless.android.sdk.stats.AppInspectionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.table.JBTable
import java.awt.Component
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableCellRenderer

class WorksTableView(tab: WorkManagerInspectorTab,
                     client: WorkManagerInspectorClient) : JBTable(WorksTableModel(client)) {
  private class WorksTableStateCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(table: JTable?,
                                               value: Any?,
                                               isSelected: Boolean,
                                               hasFocus: Boolean,
                                               row: Int,
                                               column: Int): Component {

      val state = WorkInfo.State.forNumber(value as Int)
      super.getTableCellRendererComponent(table, state.capitalizedName(), isSelected, hasFocus, row, column)
      icon = state.icon()
      return this
    }
  }

  private class WorksTableTimeCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(table: JTable?,
                                               value: Any?,
                                               isSelected: Boolean,
                                               hasFocus: Boolean,
                                               row: Int,
                                               column: Int): Component =
      super.getTableCellRendererComponent(table, (value as Long).toFormattedTimeString(), isSelected, hasFocus, row, column)
  }

  private class WorksTableDataCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(table: JTable?,
                                               value: Any?,
                                               isSelected: Boolean,
                                               hasFocus: Boolean,
                                               row: Int,
                                               column: Int): Component {
      val pair = value as Pair<*, *>
      val data = pair.second as Data
      val text = if (data.entriesList.isEmpty()) {
        if ((pair.first as WorkInfo.State).isFinished()) {
          foreground = WorkManagerInspectorColors.DATA_TEXT_NULL_COLOR
          WorkManagerInspectorBundle.message("table.data.null")

        }
        else {
          foreground = WorkManagerInspectorColors.DATA_TEXT_AWAITING_COLOR
          WorkManagerInspectorBundle.message("table.data.awaiting")
        }
      }
      else {
        foreground = null
        data.entriesList.joinToString(prefix = "{ ", postfix = " }") { "${it.key}: ${it.value}" }
      }
      super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column)
      return this
    }
  }

  init {
    autoCreateRowSorter = true

    columnModel.getColumn(WorksTableModel.Column.ORDER.ordinal).cellRenderer = DefaultTableCellRenderer()
    columnModel.getColumn(WorksTableModel.Column.CLASS_NAME.ordinal).cellRenderer = DefaultTableCellRenderer()
    columnModel.getColumn(WorksTableModel.Column.STATE.ordinal).cellRenderer = WorksTableStateCellRenderer()
    columnModel.getColumn(WorksTableModel.Column.TIME_STARTED.ordinal).cellRenderer = WorksTableTimeCellRenderer()
    columnModel.getColumn(WorksTableModel.Column.DATA.ordinal).cellRenderer = WorksTableDataCellRenderer()

    // Adjusts width for each column.
    addComponentListener(object : ComponentAdapter() {
      fun refreshColumnSizes() {
        for (column in WorksTableModel.Column.values()) {
          columnModel.getColumn(column.ordinal).preferredWidth = (width * column.widthPercentage).toInt()
        }
      }

      override fun componentShown(e: ComponentEvent) {
        refreshColumnSizes()
      }

      override fun componentResized(e: ComponentEvent) {
        refreshColumnSizes()
      }
    })

    model.addTableModelListener {
      ApplicationManager.getApplication().invokeLater {
        if (tab.selectedWork != null) {
          val tableModelRow = client.indexOfFirstWorkInfo { it.id == tab.selectedWork?.id }
          if (tableModelRow != -1) {
            val tableRow = convertRowIndexToView(tableModelRow)
            addRowSelectionInterval(tableRow, tableRow)
          }
          else {
            // Select the first row from the table when the selected work is removed.
            if (rowCount > 0) {
              addRowSelectionInterval(0, 0)
            }
            // Close the details view when the table is empty.
            else {
              tab.isDetailsViewVisible = false
              tab.selectedWork = null
            }
          }
        }
      }
    }

    selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    selectionModel.addListSelectionListener {
      if (!it.valueIsAdjusting && selectedRow != -1) {
        // Do not open details view here as the selection updates may come from model changes.
        tab.selectedWork = client.getWorkInfoOrNull(convertRowIndexToModel(selectedRow))
      }
    }

    addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        // Open details view when the table is clicked.
        // Updates for [tab.selectedWork] are handled by [selectionModel].
        if (rowAtPoint(e.point) in 0 until rowCount) {
          tab.isDetailsViewVisible = true
          client.tracker.trackWorkSelected(AppInspectionEvent.WorkManagerInspectorEvent.Context.TABLE_CONTEXT)
        }
      }
    })

    tab.addSelectedWorkListener { work ->
      if (work != null) {
        val tableModelRow = client.indexOfFirstWorkInfo { it.id == work.id }
        if (tableModelRow != -1) {
          val tableRow = convertRowIndexToView(tableModelRow)
          addRowSelectionInterval(tableRow, tableRow)
        }
      }
    }
  }
}
