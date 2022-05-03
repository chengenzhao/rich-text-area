package com.gluonhq.richtextarea.model;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.gluonhq.richtextarea.viewmodel.RichTextAreaViewModel.Direction;

public class Table {

    public static final Logger LOGGER = Logger.getLogger(Table.class.getName());

    private final String text;
    private final int start;
    private final int rows;
    private final int columns;
    private final List<Integer> positions;

    public Table(String text, int start, int rows, int columns) {
        this.text = text;
        this.start = start;
        this.rows = rows;
        this.columns = columns;

        positions = getTablePositions();
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getTableTextLength() {
        return text.length() - (text.endsWith("\n") ? 1 : 0);
    }

    public int getNextRow(int caret, Direction direction) {
        return getCurrentRow(caret) + (direction == Direction.DOWN ? 1 : 0);
    }

    public int getNextColumn(int caret, Direction direction) {
        return getCurrentColumn(caret) + (direction == Direction.FORWARD ? 1 : 0);
    }

    public int getCaretAt(int caret, Direction direction) {
        int currentRow = getCurrentRow(caret);
        if (direction == Direction.DOWN) {
            // move caret to end of current row
            return positions.get((currentRow + 1) * columns - 1);
        } else {
            // move caret to beginning of current row
            return currentRow == 0 ? start : positions.get(currentRow * columns - 1);
        }
    }

     public int getCaretAtNextRow(int caret, Direction direction) {
        int currentRow = getCurrentRow(caret);
        int currentCol = getCurrentColumn(caret);
        if (direction == Direction.DOWN) {
            // move caret to same column of next row, or after end of paragraph
            return currentRow < rows - 1 ?
                    positions.get((currentRow + 1) * columns + currentCol) : start + text.length();
        } else {
            // move caret to same column of previous row, or before start of paragraph
            return currentRow == 0 ?
                    Math.max(start - 1, 0): positions.get((currentRow - 1) * columns + currentCol);
        }
    }

    public List<Integer> selectNextCell(int caret, Direction direction) {
        int currentRow = getCurrentRow(caret);
        int currentCol = getCurrentColumn(caret);
        int currentCell = currentRow * columns + currentCol;
        System.out.println(positions);
        if (direction == Direction.FORWARD) {
            // move caret to next cell, or after end of paragraph
            return currentCell < positions.size() - 1 ?
                    List.of(positions.get(currentCell) + 1, positions.get(currentCell + 1)) :
                    List.of(start + text.length());
        } else {
            // move caret to prev cell, or before start of paragraph
            return currentCell == 0 ?
                    List.of(Math.max(start - 1, 0)) :
                    List.of(currentCell == 1 ? start : positions.get(currentCell - 2) + 1, positions.get(currentCell - 1));
        }
    }

    public int getCaretAtColumn(int column) {
        return positions.get(column);
    }

    public int getRowLength(int caret) {
        int currentRow = getCurrentRow(caret);
        return getCaretAt(caret, Direction.DOWN) - getCaretAt(caret, Direction.UP) + (currentRow == 0 ? 1 : 0);
    }

    public int getCurrentRow(int caret) {
        return (int) text.substring(0, caret - start).codePoints()
                .filter(c -> c == TextBuffer.ZERO_WIDTH_TABLE_SEPARATOR)
                .count() / columns;
    }

    public int getCurrentColumn(int caret) {
        return (int) text.substring(0, caret - start).codePoints()
                .filter(c -> c == TextBuffer.ZERO_WIDTH_TABLE_SEPARATOR)
                .count() % columns;
    }

    public String addColumnAndGetTableText(int caret, Direction direction) {
        int currentCol = getCurrentColumn(caret);
        String newText = text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
        for (int i = rows - 1; i >= 0; i--) {
            int pos;
            if (direction == Direction.FORWARD) {
                // add separator to end of current column, for each row
                pos = positions.get(i * columns + currentCol) - start;
            } else {
                // add separator to beginning of current column, for each row
                pos = currentCol == 0 && i == 0 ? 0 : positions.get(i * columns + currentCol - 1) - start;
            }
            newText = newText.substring(0, pos) + TextBuffer.ZERO_WIDTH_TABLE_SEPARATOR + newText.substring(pos);
        }
        return newText;
    }

    public String removeColumnAndGetText(int caret) {
        int currentCol = getCurrentColumn(caret);
        String newText = text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
        for (int i = rows - 1; i >= 0; i--) {
            // remove text from current column, for each row
            int posStart = currentCol == 0 && i == 0 ? 0 : positions.get(i * columns + currentCol - 1) - start;
            int posEnd = positions.get(i * columns + currentCol) - start;
            newText = newText.substring(0, posStart) + newText.substring(posEnd);
        }
        return newText;
    }

    public void printTable() {
        String tableText = text.replaceAll("" + TextBuffer.ZERO_WIDTH_TABLE_SEPARATOR, "|").replaceAll("\n", "]");
        int start = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            int end = positions.get((i + 1) * columns - 1);
            sb.append(tableText, start, end + 1);
            if (i < rows - 1) {
                sb.append("\n");
            }
            start = end;
        }
        LOGGER.fine("Table:\n" + sb);
    }

    private List<Integer> getTablePositions() {
        List<Integer> positions = IntStream.iterate(text.indexOf(TextBuffer.ZERO_WIDTH_TABLE_SEPARATOR),
                        index -> index >= 0,
                        index -> text.indexOf(TextBuffer.ZERO_WIDTH_TABLE_SEPARATOR, index + 1))
                .boxed()
                .map(i -> i + start)
                .collect(Collectors.toList());
        positions.add(start + text.length() - 1);
        return positions;
    }

}
