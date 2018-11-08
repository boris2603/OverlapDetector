package com.company;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.*;
import java.util.*;
import java.lang.Exception;

class InstallFile {

    // результаты разбора
    public String sZNI ="";
    public String Developer="";
    public String Distributive="";
    public ArrayList<DepListItem> FullPCKItemsList = new ArrayList<>();
    public ArrayList<String> DepZNIList= new ArrayList<>();
    public ArrayList<String> EmailList= new ArrayList<>();
    public ArrayList<String> AlsoReleasedZNI=new ArrayList<>();


    public String getInstallFileMasterPath() {
        return InstallFileMasterPath;
    }

    // Путь к файлам где лежит дистрибутив
    private final String InstallFileMasterPath;

    // Список ошибок
    private String HasErrorString = "";

    // Список PCK файлов
    private final ArrayList<String> pckList = new ArrayList<>();
    // Список MDB файлов
    private final ArrayList<String> mdbList = new ArrayList<>();

    // Следующая строка скорее всего разработчик
    private boolean NextLineIsDeveloper=false;

    // Список паттернов для разбора файла Install.txt
    // ЗНИ и почта разработки
    private final Pattern pZNIIndicator = Pattern.compile("(ЗНИ|RFC|ZNI_|RFC#)(\\W|\\t)*([0-9]{6})", Pattern.CASE_INSENSITIVE);
    private final Pattern pZNI = Pattern.compile("([0-9]{6})", Pattern.CASE_INSENSITIVE);
    // ЗНО/CI/C0/SD и прочая светотень
    private final Pattern pZNOIndicator = Pattern.compile("(ЗНО|C0|CI|SD|IM|ZNO)\\W*[0-9]{6,8}", Pattern.CASE_INSENSITIVE);
    private final Pattern pZNO = Pattern.compile("[0-9]{6,8}", Pattern.CASE_INSENSITIVE);
    // UNITY
    private final Pattern pUnityIndicator = Pattern.compile("(UNITY)(.*|_)[0-9]", Pattern.CASE_INSENSITIVE);
    private final Pattern pUnity = Pattern.compile("[0-9]{6}", Pattern.CASE_INSENSITIVE);

    // Почта
    private final Pattern pEMail = Pattern.compile("\\w+(\\w?[\\.|-]*\\w+)+@(\\w+[\\.|-]?\\w+)+\\.[.a-zA-Z]{2,8}", Pattern.CASE_INSENSITIVE);

    // Так-же реализованные ЗНИ
    private final Pattern pAlsoReleasedIndicator = Pattern.compile("Так же реализованы ЗНИ", Pattern.CASE_INSENSITIVE);

    // PCK файлы для установки
    private final Pattern pPCKListIndicator = Pattern.compile("(Установить|Устанавливаем)", Pattern.CASE_INSENSITIVE);
    private final Pattern pPCK = Pattern.compile("([\\w-]+\\\\)*((ЗНО|C0|CI|SD|IM))*[_A-Za-z0-9-]*\\.pck", Pattern.CASE_INSENSITIVE);
    // Список ЗНИ зависимостей
    private final Pattern pDepListIndicator = Pattern.compile("(Установка|Установить|Ставить|Устанавливать)\\s*(после|до)", Pattern.CASE_INSENSITIVE);
    // Индикатор что есть Mdb
    private final Pattern pMDB = Pattern.compile("([\\w-]+\\\\)*[_A-Za-z0-9-]*\\.mdb", Pattern.CASE_INSENSITIVE);
    // Индикатор Разработчик
    private final Pattern pDeveloperSign = Pattern.compile("(Разработчик)(и?)",Pattern.CASE_INSENSITIVE );
    // Разаработчик написано в одной строке
    private final Pattern pDeveloper = Pattern.compile("(Разработчики|Разработчик)\\s*:*\\s*(.{1,})",Pattern.CASE_INSENSITIVE );

    // Индикатор, что у нас есть ошибки парсинга файлов
    boolean HasError()
    {
        return (!HasErrorString.isEmpty());
    }

    // Вернуть список ошибок парсинга
    public String getHasErrorString() {
        return ("ERROR "+ InstallFileMasterPath + System.lineSeparator() + HasErrorString);
    }

    InstallFile(String InstallTxtFileName)
    {

        String FileSeparator=(String)System.getProperty("file.separator");

        InstallFileMasterPath = Paths.get(InstallTxtFileName).getParent().toString();
        Distributive=InstallFileMasterPath.substring(InstallFileMasterPath.lastIndexOf(FileSeparator)+1);

        InstallFileParce(LoadFile(InstallTxtFileName));
        if (sZNI.isEmpty())
        {
            HasErrorString = HasErrorString + " Invalid install.txt format: unresolve ЗНИ parametr" + System.lineSeparator();
        }
        else if (pckList.isEmpty() && (!mdbList.isEmpty()))
        {
            HasErrorString = HasErrorString + " Invalid install.txt format: found mdm but no pck files" + System.lineSeparator();
        }
        else
        {
            LoadDeployObjects(pckList); // заполняем структуру с перечнем объектов из всех pck этого install
        }

    }

    // Распарсить строку Install
    private void InstallFileParce(List<String> lines) {

        for(String line: lines){

            // Получаем номер ЗНИ
            if (sZNI.isEmpty())
            {
                AlsoReleasedZNI.addAll(GetMatchListByIndicator(line, pZNI, pZNIIndicator));
                if (sZNI.isEmpty() && !AlsoReleasedZNI.isEmpty())
                {
                    sZNI=AlsoReleasedZNI.get(0);
                    AlsoReleasedZNI.remove(0);
                }
                if (sZNI.isEmpty())
                {
                    AlsoReleasedZNI.addAll(GetMatchListByIndicator(line, pUnity, pUnityIndicator));
                    if (!AlsoReleasedZNI.isEmpty()) {
                        sZNI = AlsoReleasedZNI.get(0);
                        AlsoReleasedZNI.remove(0);
                    }
                }
                if (sZNI.isEmpty())
                {
                    AlsoReleasedZNI.addAll(GetMatchListByIndicator(line, pZNO, pZNOIndicator));
                    if (!AlsoReleasedZNI.isEmpty()) {
                        sZNI = AlsoReleasedZNI.get(0);
                        AlsoReleasedZNI.remove(0);
                    }
                }
                if (!sZNI.isEmpty())
                {
                    sZNI=sZNI.replace(' ','_');
                }
            }
            // Получаем ФИО разработчика
            if (Developer.isEmpty())
            {

                if (PatternFound(line,pDeveloperSign)) {
                    Developer = GetMatchParam(line, pDeveloper, 2);
                    Developer = Developer.replace(',',' ');
                    Developer = Developer.trim();
                    if (Developer.length()<4) { Developer=""; }; // Костыль по определению разработчика, 2 группа равна : если в install  указано Разработчики:
                    NextLineIsDeveloper=Developer.isEmpty();
                }
                else
                    if (NextLineIsDeveloper)
                    {
                        Developer=line.replace(',',' ');
                        Developer=Developer.trim();
                    }

                // Получим только ФИО разарботчика все состально е отбросим
                if (!Developer.isEmpty()) {
                    String[] sDeveloper=Developer.split("[[ ]*|[//.]]");

                    Developer="";
                    for (int idx=0;idx<3;idx++)
                        if (idx<sDeveloper.length) {
                            Developer = Developer  + sDeveloper[idx] + ((sDeveloper[idx].length()==1) ? "." : " ");
                        }
                     Developer=Developer.trim();
                    }
            }

            // Создаем список PCK
            pckList.addAll(GetMatchListByIndicator(line, pPCK, pPCKListIndicator));
            mdbList.addAll(GetMatchList(line, pMDB));

            //Обрабатываем Установить после
            DepZNIList.addAll(GetMatchListByIndicator(line, pZNI, pDepListIndicator));

            //Обрабатываем Так же реализованы ЗНИ
            AlsoReleasedZNI.addAll(GetMatchListByIndicator(line, pZNI, pAlsoReleasedIndicator));

            //Получаем список почты
            if (line.contains ("@"))
                EmailList.addAll(GetMatchList(line, pEMail));
        }

    }

    // Встречается-ли паттерн в строке
    private boolean PatternFound(String line, Pattern pPattern)
    {
        Matcher m = pPattern.matcher(line);
        return m.find();
    }

    // Получить массив подстрок в котором встечается pPattern
    private ArrayList<String> GetMatchList(String line, Pattern pPattern)
    {
        ArrayList<String> ResultList = new ArrayList<>();

        Matcher m = pPattern.matcher(line);
        while (m.find()) {
            ResultList.add(m.group());
        }
        return ResultList;
    }

    // Получить маасив подстрок по pPattern c учетом сдвига CorrectIndex, если найден pPatternIndicator
    private ArrayList<String> GetMatchListByIndicator(String line, Pattern pPattern, Pattern pPatternIndicator)
    {
        ArrayList<String> ResultList = new ArrayList<>();

        Matcher m = pPatternIndicator.matcher(line);
        if (m.find()) {
            ResultList = GetMatchList(line,pPattern);
        }
        return ResultList;
    }

    private String GetMatchParam(String line, Pattern pPattern, int MatchGroupIdx) {
        String Result = "";

        Matcher m = pPattern.matcher(line);
        if (m.find())  {
            Result = m.group(MatchGroupIdx);
        }
        return Result;
    }

    // Загрузить в переменную список объектов из списка PCK файлов
//!!!SAM проверить обработку  РСК-файлов в вложенных папках
    private void LoadDeployObjects(List<String> pckFileList)
    {
        for (String pckFile : pckFileList) {
            File file = new File(InstallFileMasterPath, pckFile.replace('\\', File.separatorChar));
            FullPCKItemsList.addAll(LoadPCKFile(file.getPath()));
        }

        // Если есть реализованные совместно ЗНИ размножим объекты
        if (!AlsoReleasedZNI.isEmpty())
        {
            ArrayList<DepListItem> masterPCKItemsList = new ArrayList<DepListItem>();
            masterPCKItemsList.addAll(FullPCKItemsList);

            for (String ZNIItem  : AlsoReleasedZNI) {
                masterPCKItemsList.forEach((item) -> {
                        try {
                            DepListItem itemPCKListItem = (DepListItem) item.clone();
                            itemPCKListItem.ZNI = ZNIItem;
                            if (!ObjectAlreadyInList(itemPCKListItem))
                                FullPCKItemsList.add(itemPCKListItem);
                        }
                       catch (CloneNotSupportedException e)
                       {
                           System.out.println("Couldn't clone list due to " + e.getMessage());
                       }
                });
            }
        }
    }

    // Распарсить один из найденных PCK
    private ArrayList<DepListItem> LoadPCKFile(String pckFileName)
    {
        ArrayList<DepListItem> DepList = new ArrayList<>();
        List<String> PCKlines = LoadFile(pckFileName);

        for (String line : PCKlines) {
            String[] items = line.split(" ");
            if (items.length > 0) {
                switch (items[0]) {
                    case "METH":
                    case "CRIT":
                    case "TRANS":
                    case "E-METH":
                    case "IDX":
                    case "ATTR":
                        DepListItem dplItem = new DepListItem();
                        dplItem.ZNI = sZNI;
                        if (items.length==3) {
                            dplItem.Object = items[2];
                            dplItem.TBP = items[1];
                        }
                        else
                        {
                            dplItem.Object = items[1];
                            dplItem.TBP = "";
                        };
                        dplItem.Type = items[0];
                        if (!ObjectAlreadyInList(dplItem)) {
                            DepList.add(dplItem);
                        }
                        break;
                }
            }
        }
        return DepList;
    }

    // Загрузить файл с диска с обработкой ошибок
    private List<String> LoadFile(String FileName) {
        List<String> lines = new ArrayList<>();

        if (!Files.exists(Paths.get(FileName)))
            {
            System.out.println("File not found " + FileName);
            HasErrorString = HasErrorString + "File not found " + FileName + System.lineSeparator();
            }
        else
            {
                try {
                    lines = Files.readAllLines(Paths.get(FileName), Charset.forName("UTF-8"));
                    } catch (UnmappableCharacterException | MalformedInputException UECEx) {
                            try {
                            lines = Files.readAllLines(Paths.get(FileName),Charset.forName("windows-1251"));
                            } catch (Exception e) {
                            System.out.println("IO Error reading file " + FileName);
                            System.out.println(e.getMessage());
                            HasErrorString = HasErrorString + "IO error reading File " + FileName + System.lineSeparator();
                        }
                }
                catch (IOException IOEx)
                {
                    System.out.println("IO Error reading file " + FileName);
                    System.out.println(IOEx.getMessage());
                    HasErrorString = HasErrorString + "IO error reading File " + FileName + System.lineSeparator();
                }
            }
        return lines;
    }

    // Проверить наличие объекта в остальном списке объектов этой ЗНИ
    private boolean ObjectAlreadyInList(DepListItem checkItem)
    {
        boolean retVal = false;
        if (checkItem != null)
        {
            for (DepListItem item : FullPCKItemsList) {
                if (item.Type.equals(checkItem.Type) &&
                        item.TBP.equals(checkItem.TBP) && item.Object.equals(checkItem.Object) && item.ZNI.equals(checkItem.ZNI))
                {
                    retVal = true;
                    break;
                }
            }
        }
        return retVal;
    }
}