package com.company;

class DepListItem implements Cloneable{
    String ZNI;
    String TBP;
    String Object;
    String Type;

    // Проверяем что объекты идентичны не учитывая номер ЗНИ
    boolean DepObjectsCheck(DepListItem checkItem) {
        if (checkItem == null || checkItem.ZNI.equals(this.ZNI))
            return false;
        else
            return (this.Type.equals(checkItem.Type) && this.TBP.equals(checkItem.TBP) && this.Object.equals(checkItem.Object));
    }

    // Метод клонирования объекта
    @Override
    public DepListItem clone() throws CloneNotSupportedException{
        DepListItem clone = (DepListItem) super.clone();

        clone.ZNI = this.ZNI;
        clone.TBP =  this.TBP;
        clone.Object = this.Object;
        clone.Type = this.Type;
        return clone;
    }
}
