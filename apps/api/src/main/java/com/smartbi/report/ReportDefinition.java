package com.smartbi.report;
import java.util.*;

public record ReportDefinition(int rows,int columns,List<Cell> cells,List<Merge> merges,Map<Integer,Integer> rowHeights,Map<Integer,Integer> columnWidths){
 public ReportDefinition{cells=cells==null?List.of():List.copyOf(cells);merges=merges==null?List.of():List.copyOf(merges);rowHeights=rowHeights==null?Map.of():Map.copyOf(rowHeights);columnWidths=columnWidths==null?Map.of():Map.copyOf(columnWidths);}
 public record Cell(int row,int column,String value,CellStyle style,Binding binding){}
 public record CellStyle(Boolean bold,String color,String background,String align){}
 public record Binding(UUID datasetId,String field,Expansion expansion,String groupBy,Aggregate aggregate){}
 public enum Expansion{NONE,DOWN,RIGHT}
 public enum Aggregate{NONE,SUM,COUNT}
 public record Merge(int startRow,int startColumn,int endRow,int endColumn){}
}
