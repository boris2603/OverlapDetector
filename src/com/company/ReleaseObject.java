package com.company;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.nio.file.*;

class ReleaseObject
{
    // Список всех объектов по ЗНИ
    private final ArrayList<DepListItem> ReleaseFullItemsList = new ArrayList<>();
    // Спосок всех ЗНИ и отмеченных зависимостей
    private final ArrayList<DepZNIListItem> ReleaseFullDepZNIList = new ArrayList<>();
    // Список зависимостей по проверяемой ЗНИ
    private ArrayList<OverlapItem> ZNIIntersectionList = new ArrayList<>();

    private final String FileObjectName = "ODObjectList.txt";
    private final String FileZNIName    = "ODZNIList.txt";
    private final String FileNamePath;

    public boolean isErrorDetected() {
        return ErrorDetected;
    }

    private boolean ErrorDetected;

    ReleaseObject(String ODFileNamePath)
    {
        FileNamePath=ODFileNamePath;
        ErrorDetected = false;

        LoadItemList(); // считываем файл с объектами релиза
        LoadZNIList();  // считываем файл с перечнем ЗНИ и зависимостей
    }


    // Загрузить список ЗНИ по релизу
    @SuppressWarnings("ManualArrayToCollectionCopy")
    void LoadZNIList()
    {
        List<String> lines = new ArrayList<>();

        if (Files.exists(Paths.get( FileNamePath, FileZNIName))) try {
            lines = Files.readAllLines(Paths.get(FileNamePath, FileZNIName), Charset.forName("windows-1251"));
        } catch (IOException e) {
            System.out.println("IO Error reading Objects File " + FileZNIName);
            System.out.println(e.getLocalizedMessage());
        }

        ReleaseFullDepZNIList.clear();

        for(String line : lines) {
            String[] items = line.split(",");
            if (items.length > 0) {
                DepZNIListItem Item = new DepZNIListItem(items[0],"");
                if (items.length > 1) {
                    Item.Developer = items[1];
                }

                for (int idx = 2; idx < items.length; idx++) {
                    Item.DependenceList.add(items[idx]);
                }
                ReleaseFullDepZNIList.add(Item);
            }
        }
    }

    // Загрузить список объектов и зависимых ЗНИ по релизу
    void LoadItemList()
    {
        List<String> lines = new ArrayList<>();

        if (Files.exists(Paths.get(FileNamePath,FileObjectName))) try {
            lines = Files.readAllLines(Paths.get(FileNamePath, FileObjectName), Charset.forName("windows-1251"));
        } catch (IOException e) {
            System.out.println("IO Error reading Objects File " + FileObjectName);
            System.out.println(e.getLocalizedMessage());
        }

        ReleaseFullItemsList.clear();

        for(String line : lines) {
            String[] items = line.split(" ");
            DepListItem Item = new DepListItem();

            Item.ZNI = items[0];
            Item.Type = items[1];
            Item.TBP = items[2];
            Item.Object = items[3];

            ReleaseFullItemsList.add(Item);
        }
    }

    //Получить список ЗНИ которые зависят от анализируемой

    public ArrayList<String> GetDependenceZNIList(InstallFile CheckInstFile)
    {
        ArrayList<String> retVal = new ArrayList<>();

        for (DepZNIListItem item : ReleaseFullDepZNIList)
            if (!item.DependenceList.isEmpty())
                    for (String itemDepList : item.DependenceList)
                        if (itemDepList.equals(CheckInstFile.sZNI)) retVal.add(item.ZNI);
        return retVal;
    }

    // Выгрузить список ЗНИ по релизу
    void SaveZNIList()
    {
        ArrayList<String> ObjectList = new ArrayList<>();

        for(DepZNIListItem item : ReleaseFullDepZNIList)
        {
            StringBuilder saveString = new StringBuilder(item.ZNI);
            saveString.append(",").append(item.Developer);

            for (String itmZNI : item.DependenceList)
            {
                if (!itmZNI.isEmpty()) {
                    saveString.append(",").append(itmZNI);
                }
            }
            // saveString=item.ZNI+" "+saveString;
            ObjectList.add(saveString.toString());
        }

        try
        {
            Files.deleteIfExists(Paths.get(FileNamePath, FileZNIName));
            Files.write(Paths.get(FileNamePath,FileZNIName), ObjectList, Charset.forName("windows-1251"), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        }
        catch (IOException e)
        {
            System.out.println("IO Error writing Objects File "+FileZNIName);
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
            //noinspection ThrowablePrintedToSystemOut
            System.out.println(e.fillInStackTrace());
        }

    }

    // Выгрузить список объектов и зависимых ЗНИ по релизу
    @SuppressWarnings("ThrowablePrintedToSystemOut")
    void SaveItemList()
    {
        ArrayList<String> ObjectList = new ArrayList<>();

        for(DepListItem item : ReleaseFullItemsList)
            if (!item.ZNI.isEmpty()) ObjectList.add(item.ZNI + " " + item.Type + " " + item.TBP + " " + item.Object);

        try
        {
            Files.deleteIfExists(Paths.get(FileNamePath, FileObjectName));
            Files.write(Paths.get(FileNamePath,FileObjectName), ObjectList, Charset.forName("windows-1251"), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        }
        catch (IOException e)
        {
            System.out.println("IO Error writing Objects File "+FileObjectName);
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
            System.out.println(e.fillInStackTrace());
        }
    }

    // Список неразрешенных зависимостей по ЗНИ
    public void OverlapDetector(InstallFile CheckInstallFile)
    {
        // Список неразрешенных зависимостей по ЗНИ и объектам
        ZNIIntersectionList = new ArrayList<>();
        String CheckZNI = "";

        // Получаем список всех зависимых объектов
        ArrayList<DepListItem> IntersectionObjects = OverlapCandidatDetector(CheckInstallFile.FullPCKItemsList);

        //смотрим список всех зависимых объектов; Проверяем что объекты по ЗНИ учтены в списке работ
        for (DepListItem itemObject: IntersectionObjects)
        {
            // Проверяем что ЗНИ в объекте учтена в списке зависимых ЗНИ

            // если зависимости не учтены в описаниях
            if (!CheckInterseptZNI(CheckInstallFile, itemObject.ZNI))
            {
                boolean makeNewItem = true;

                for (OverlapItem item : ZNIIntersectionList) {
                    if (item.mainZNI.equals(itemObject.ZNI)){
                        item.depListItems.add(itemObject);
                        makeNewItem = false;
                    }
                }

                if (makeNewItem){
                    OverlapItem  ZNIIntersectionItem = new OverlapItem(itemObject.ZNI);
                    ZNIIntersectionItem.depListItems.add(itemObject);
                    ZNIIntersectionList.add(ZNIIntersectionItem);
                }
            }
        }

        // Проверим что ЗНИ от которых есть зависимость передавалось на стенд
        if (!CheckInstallFile.DepZNIList.isEmpty())
            for (String TestZNI : CheckInstallFile.DepZNIList)
                if (!AlreadyInstallZNI(TestZNI)) {
                    // OverlapItem ZNIIntersectionItem = new OverlapItem(TestZNI);
                    ZNIIntersectionList.add(new OverlapItem(TestZNI));
                }

        // Запомним имя разработчика
        for (DepZNIListItem itemZNI : ReleaseFullDepZNIList)
        {
            for (OverlapItem item : ZNIIntersectionList) {
              if (itemZNI.ZNI.equals(item.mainZNI)) {
                  item.Developer = itemZNI.Developer;
              }
            }
        }
        // Проверим что есть ошибки
        ErrorDetected=!ZNIIntersectionList.isEmpty();

        // return ZNIIntersectionList;
    }

    // Проверим что ЗНИ устанавливалось на стенд
    private boolean AlreadyInstallZNI(String CheckZNI)
    {
        boolean retval=false;
        for (DepZNIListItem itemZNI : ReleaseFullDepZNIList)
            if (itemZNI.ZNI.equals(CheckZNI)) {
                retval = true;
                break;
            }
        return retval;
    }

    // Если по ЗНИ учтены зависимости = true
    private boolean CheckInterseptZNI(InstallFile ChangeFileZNI, String DependZNI){

        boolean retval = false;
        // проверяем зависимость в install-кандидате
        for (String item: ChangeFileZNI.DepZNIList ){
            if(item.equals(DependZNI)){
                retval = true;
                break;
            }
        }

        // ищем по списку релиза
        if(!retval) {
            // проходим по списку ЗНИ релиза с перечнями их зависимых
            for (DepZNIListItem depZNI : ReleaseFullDepZNIList){
                // найти ЗНИ кандидат в составе релиза и проверить в ее зависимых искомую
                if (depZNI.ZNI.equals(ChangeFileZNI.sZNI) && depZNI.CheckZNI(DependZNI) ) {
                    retval = true;
                    break;
                }
                // найти искомую ЗНИ и искать в ее зависимых ЗНИ-кандидат
                if (depZNI.ZNI.equals(DependZNI) && depZNI.CheckZNI(ChangeFileZNI.sZNI) ) {
                    retval = true;
                    break;
                }
            }
        }
        return retval;
    }

    // Заменить список ЗНИ в релизе
    void ChangeReleaseZNIList(InstallFile ChangeFile)
    {
        DepZNIListItem RemoveItem = new DepZNIListItem("","");

        for(DepZNIListItem item : ReleaseFullDepZNIList)
            if (item.ZNI.equals(ChangeFile.sZNI)) {
                RemoveItem = item;
                break;
            }
        if (!RemoveItem.ZNI.isEmpty()) ReleaseFullDepZNIList.remove(RemoveItem);

        DepZNIListItem AddItem = new DepZNIListItem(ChangeFile.sZNI, ChangeFile.Developer);
        AddItem.DependenceList = ChangeFile.DepZNIList;
//SAM зачем условие ??
        if (!AddItem.ZNI.isEmpty()) ReleaseFullDepZNIList.add(AddItem);
    }

    // Заменить список объектов по ЗНИ в обзщем релизе
    void ChangeReleaseItemList(InstallFile ChangeFile)
    {
        ArrayList<DepListItem> RemoveFullItemsList = new ArrayList<>();
        //Удалить все записи по ЗНИ
        for (DepListItem Item : ReleaseFullItemsList)
            if (Item.ZNI.equals(ChangeFile.sZNI))
                RemoveFullItemsList.add(Item);
        // Добавляем свежие объекты
        ReleaseFullItemsList.removeAll(RemoveFullItemsList);
        ReleaseFullItemsList.addAll(ChangeFile.FullPCKItemsList);
    }

    // Определить список объектов по которым есть пересечения
    private ArrayList<DepListItem> OverlapCandidatDetector(ArrayList<DepListItem> pZNIObjectList)
    {
        ArrayList<DepListItem> OverlapItems = new ArrayList<>();
        // проходим по списку изменяемых объектов у ЗНИ-кандидата
        // собираем перечень
        for(DepListItem Item:pZNIObjectList)
        {
            ArrayList<DepListItem> itemCandidate = FindObjects(Item);
            OverlapItems.addAll(itemCandidate);
        }
        return OverlapItems;
    }

    // Найти такие же изменяемые объекты в других ЗНИ из состава основного релиза
    private ArrayList<DepListItem> FindObjects(DepListItem checkItem)
    {
        ArrayList<DepListItem> OverlapItems = new ArrayList<>();

        for(DepListItem Item: ReleaseFullItemsList)
            if (Item.DepObjectsCheck(checkItem) && !Item.ZNI.equals(checkItem.ZNI)) OverlapItems.add(Item);
        return OverlapItems;
    }

    // Сгенерировать текст отчета по проверке
    public String GenerateReportText(InstallFile CheckInstFile, boolean MachineReadyFormat) {

        // ArrayList<OverlapItem> ZNIDepend = this.OverlapDetector(CheckInstFile);
        String LogFileText =  new String();

        if (!ZNIIntersectionList.isEmpty())
        {
            if (!MachineReadyFormat) LogFileText=LogFileText.concat(String.format("\nNot allowed intersections by RFC %s %s:\n",CheckInstFile.sZNI,CheckInstFile.Developer));
            for (OverlapItem item : ZNIIntersectionList) {
                if (!MachineReadyFormat) LogFileText=LogFileText.concat(String.format("%s %s",item.mainZNI,item.Developer));
                if (item.depListItems.isEmpty()) {
                    LogFileText= MachineReadyFormat ? LogFileText.concat(String.format("%s %s,1,%s %s\n",CheckInstFile.sZNI,CheckInstFile.Developer,item.mainZNI,item.Developer)) : LogFileText.concat(" not installed \n");
                } else {
                    for (DepListItem depListItem : item.depListItems) {
                        LogFileText= MachineReadyFormat ? LogFileText.concat(String.format("%s %s,2,%s %s, %s %s %s \n",CheckInstFile.sZNI,CheckInstFile.Developer, depListItem.ZNI, item.Developer,depListItem.Type,depListItem.TBP,depListItem.Object)) :
                                            LogFileText.concat(String.format("\n  %s %s %s %s \n",depListItem.ZNI,depListItem.Type,depListItem.TBP,depListItem.Object));
                    }
                }
            }
        }
        else
        {
            LogFileText=LogFileText.concat(String.format("\n %s intersection check passed successfully"));


            ArrayList<String> InformZIN=this.GetDependenceZNIList(CheckInstFile);
            if (!InformZIN.isEmpty())
            {
                LogFileText=LogFileText.concat("WARNING!!! Report the changes in RFC: ");
                for (String item :InformZIN)
                {
                    LogFileText=LogFileText.concat(item.concat(" "));
                }
                LogFileText=LogFileText.concat("\n");
            }
        }

    return LogFileText;
    }

    // Записать отчет в файл
    public void SaveReport(String LogFileName, InstallFile CheckInsFile, boolean AppendLogFile)
    {
        String ReportText = "";

        if (CheckInsFile.HasError())
        {
            ReportText = String.format("%s,3,%s",CheckInsFile.sZNI,CheckInsFile.getInstallFileMasterPath());
        }
        else ReportText = this.GenerateReportText(CheckInsFile, true);

        try
        {
            if (AppendLogFile)
            {
                if (!Files.exists(Paths.get(LogFileName)))
                    Files.write(Paths.get(LogFileName), ReportText.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                else Files.write(Paths.get(LogFileName), ReportText.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            }
            else {
                Files.deleteIfExists(Paths.get(LogFileName));
                Files.write(Paths.get(LogFileName), ReportText.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            };
        }
        catch (IOException e)
        {
            System.out.println("IO Error writing Objects File "+LogFileName);
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
            System.out.println(e.fillInStackTrace());
        }
    }
}
