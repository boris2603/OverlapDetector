package com.company;

class DepListItem {
    String ZNI;
    String TBP;
    String Object;
    String Type;

    // Проверяем что объекты идентичны не учитывая номер ЗНИ
    boolean DepObjectsCheck(DepListItem checkItem) {
        return checkItem != null && (this.Type.equals(checkItem.Type) && this.TBP.equals(checkItem.TBP) && this.Object.equals(checkItem.Object));
    }
}
