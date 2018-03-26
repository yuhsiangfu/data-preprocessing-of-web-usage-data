/***********************************/
/* DataPreprocessing               */
/*---------------------------------*/
/* Ver.4,Date:081217               */
/* Ver.5,Date:090612               */
/*       Check:180326              */
/*---------------------------------*/
/* Program by Fu,Yu-Hsiang in Aizu */
/***********************************/

//Import
import java.lang.*;
import java.io.*;
import java.util.*;
import java.text.*;

//DataPreprocessing
public class DataPreprocessing{
    class Pattern{//Pattern:<ip,time,category,page,hash_index>
        String user_ip,category,page;
        int time,hash_index;

        public Pattern(){}
    }

    class Category{//Category
        String category;
        ArrayList<String> pages = new ArrayList<String>();

        public Category(String s){
            category=s;
        }
    }

    class User{//User
        String user_ip;
        ArrayList<Pattern> patterns = new ArrayList<Pattern>();

        public User(String s){
            user_ip=s;
        }
    }

    class Chain{//Chain of hash table
        ArrayList<User> users = new ArrayList<User>();//User list
    }

///////////////////////////////////////////////////////////////////////////////

    private String filename,pathin,pathout;
    private int level,timeout,hash_modular;
    private int total_user,total_session,total_category,total_page;
    private ArrayList<Category> categories;
    private ArrayList<Pattern> clean_data;
    private ArrayList<Chain> hash_table;

    //DataPreprocessing
    public DataPreprocessing(String s1,String s2,String s3,String s4){//log file,level,timeout,hash_table_size
        System.out.println("-Data preprocessing");

        long startTime = System.currentTimeMillis();//Start time

        Initialization(s1,s2,s3,s4);//Initialization
        DataCleaning();//DataCleaning
        UserIdentification();//UserIdentification
        UserSessionIdentification();//UserSessionIdentification
        PathCompletion();//PathCompletion

        float endTime = (float)(System.currentTimeMillis()-startTime)/1000F;//End time
        System.out.println(endTime);
    }

    //Initialization
    private void Initialization(String s1,String s2,String s3,String s4){
        System.out.println(" -0.Initialization");

        filename=s1.substring(0,s1.indexOf("."));
        pathin="input\\"+filename+".txt";
        pathout="output\\"+filename+".txt";

        level=Integer.parseInt(s2);//Specific level of category
        timeout=Integer.parseInt(s3);//Timeout
        hash_modular=Integer.parseInt(s4);//Also size of hash table
    }

    //DataCleaning
    private void DataCleaning(){
        System.out.println(" -1.DataCleaning");

        categories = new ArrayList<Category>();//Category index
        clean_data = new ArrayList<Pattern>();//Clean data

        try{
            BufferedReader br = new BufferedReader(new FileReader(pathin));
            String s="";

            while((s=br.readLine())!=null){
                boolean request=false,html=false,status=false;

                StringTokenizer st = new StringTokenizer(s.toLowerCase());//Lower case
                int stTokens = st.countTokens();
                String[] tokens = new String[stTokens];

                for(int a=0;a<stTokens;a++){
                    tokens[a]=st.nextToken();
                }

                if(tokens.length==10){//Correct number of tokens
                    if(tokens[5].indexOf("get")!=-1){//Request=get
                        request=true;
                    }
                    if(tokens[6].endsWith(".html")){//HTML page
                        html=true;
                    }
                    if(tokens[8].indexOf("200")!=-1){//Status code=200(ok)
                        status=true;
                    }

                    if(request && html && status){//Cleaned data
                        clean_data.add(Formating(tokens));
                    }
                }
            }
            br.close();
        }
        catch(Exception e){
            System.out.println(e);
            System.out.println("DataPreprocessing-DataCleaning:¨Ò¥~¿ù»~!!");
        }
    }

    //Formating
    private Pattern Formating(String[] s){//Tokens
        Pattern new_pattern = new Pattern();//New pattern of clean data

        //User IP
        new_pattern.user_ip=s[0];

        //Hash index
        String first_ip="";

        if(s[0].indexOf(".")!=-1){//User ip
            first_ip=s[0].substring(0,s[0].indexOf("."));
        }
        else{//User id
            first_ip=s[0];
        }

        char[] c=first_ip.toCharArray();

        for(int a=0;a<c.length;a++){
            new_pattern.hash_index+=c[a];
        }

        //Time
        int day=Integer.parseInt(s[3].substring(s[3].indexOf("[")+1,s[3].indexOf("/")));//Day
        int hour=Integer.parseInt(s[3].substring(s[3].indexOf(":")+1,s[3].indexOf(":")+3));//Hour
        int min=Integer.parseInt(s[3].substring(s[3].lastIndexOf(":")-2,s[3].lastIndexOf(":")));//Min
        new_pattern.time=((day-1)*24*60)+(hour*60)+min;

        //Category, page
        StringTokenizer st = new StringTokenizer(s[6],"/");//Lower case
        int stTokens = st.countTokens();

        if(stTokens==1){//Add "/root"
            s[6]="/root"+s[6];
            st = new StringTokenizer(s[6],"/");
            stTokens = st.countTokens();
        }

        String[] tokens = new String[stTokens];

        for(int a=0;a<stTokens;a++){
            tokens[a]=st.nextToken();
        }

        //Setup target level of category
        int target_level;
        int cl=tokens.length-2;//Category level
        int pl=cl+1;//Page level

        if(cl<level){//cl<level
            target_level=cl;
        }
        else{//else
            target_level=level;
        }

        //Get category and page
        new_pattern.category=tokens[target_level];//Category Level
        new_pattern.page=tokens[pl];//Page Level

        //Identifing category and page
        int index_c=FindCategory(tokens[target_level]);

        if(index_c!=-1){//Find category
            if(FindPage(index_c,tokens[pl])==-1){//Not find page
                categories.get(index_c).pages.add(tokens[pl]);
            }
        }
        else{//Not find category
            Category new_category = new Category(tokens[target_level]);
            new_category.pages.add(tokens[pl]);
            categories.add(new_category);
        }

        return new_pattern;
    }

    //FindCategory
    private int FindCategory(String s){
        for(int a=0;a<categories.size();a++){
            if(categories.get(a).category.equals(s)){
                return a;
            }
        }

        return -1;
    }

    //FindPage
    private int FindPage(int x,String y){
        for(int a=0;a<categories.get(x).pages.size();a++){
            if(categories.get(x).pages.get(a).equals(y)){
                return a;
            }
        }

        return -1;
    }

    //UserIdentification
    private void UserIdentification(){
        System.out.println(" -2.UserIdentification");

        hash_table = new ArrayList<Chain>();//Hash table

        for(int a=0;a<hash_modular;a++){//Add chains into hash table
            Chain new_chain = new Chain();
            hash_table.add(new_chain);
        }

        for(int a=0;a<clean_data.size();a++){
            int index_hash=clean_data.get(a).hash_index%hash_modular;//Hash function, H(x)=hash_index mod hash_modular
            int index_user=FindUser(clean_data.get(a).user_ip,index_hash);

            if(index_user!=-1){//Find uesr
                hash_table.get(index_hash).users.get(index_user).patterns.add(clean_data.get(a));
            }
            else{//Not find user
                User new_user = new User(clean_data.get(a).user_ip);
                new_user.patterns.add(clean_data.get(a));
                hash_table.get(index_hash).users.add(new_user);
            }
        }

        clean_data=null;
        System.gc();
    }

    //FindUser
    private int FindUser(String s,int x){
        for(int a=0;a<hash_table.get(x).users.size();a++){
            if(hash_table.get(x).users.get(a).user_ip.equals(s)){
                return a;
            }
        }

        return -1;
    }

    //UserSessionIdentification
    private void UserSessionIdentification(){
        System.out.println(" -3.UserSessionIdentification");

        for(int a=0;a<hash_table.size();a++){//Size of hash table
            total_user+=hash_table.get(a).users.size();//Total number of user

            for(int b=0;b<hash_table.get(a).users.size();b++){//Size of each hash slot of users(chain)
                int index_start=-1;
                int time_first=hash_table.get(a).users.get(b).patterns.get(0).time;
                //int time_pre=0,time_now=0;

                for(int c=1;c<hash_table.get(a).users.get(b).patterns.size();c++){//Size of each user's pages
                    int time_now=hash_table.get(a).users.get(b).patterns.get(c).time;

                    if(c!=0){//Second to last page
                        if(Math.abs(time_now-time_first)>timeout){//Greater than timeout
                            index_start=c;
                            break;
                        }
                    }

                    //time_pre=time_now;
                    //time_now=0;
                }

                if(index_start!=-1){//Create new session
                    int index_end=hash_table.get(a).users.get(b).patterns.size();
                    User new_user = new User(hash_table.get(a).users.get(b).user_ip);//New session

                    for(int c=index_start;c<index_end;c++){//Add into new session
                        new_user.patterns.add(hash_table.get(a).users.get(b).patterns.get(c));

                    }

                    for(int c=(index_end-1);c>=index_start;c--){
                        hash_table.get(a).users.get(b).patterns.remove(c);//Remove from old session
                    }

                    hash_table.get(a).users.add(b+1,new_user);//Add new session to the end of list
                }
            }
        }
    }

    //PathCompletion
    private void PathCompletion(){
        System.out.println(" -4.PathCompletion");

        try{
            //Statistic
            total_category=categories.size();
            total_page=0;

            int[] bound = new int[total_category];
            double avg_length=0;

            for(int a=0;a<categories.size();a++){//Category bound
                total_page+=categories.get(a).pages.size();

                if(a==0){
                    bound[a]=categories.get(a).pages.size();
                }
                else{
                    bound[a]=bound[a-1]+categories.get(a).pages.size();
                }
            }

            //Output path
            BufferedWriter bw = new BufferedWriter(new FileWriter(pathout));

            for(int a=0;a<hash_table.size();a++){
                total_session+=hash_table.get(a).users.size();//Total number of session

                for(int b=0;b<hash_table.get(a).users.size();b++){
                    avg_length+=hash_table.get(a).users.get(b).patterns.size();//Avg. length of session
                    String path="";

                    for(int c=0;c<hash_table.get(a).users.get(b).patterns.size();c++){
                        int index_c=FindCategory(hash_table.get(a).users.get(b).patterns.get(c).category);
                        int index_p=FindPage(index_c,hash_table.get(a).users.get(b).patterns.get(c).page);

                        path+=(index_c+1)+",";

                        if(index_c==0){
                            path+=(index_p+1)+" ";
                        }
                        else{
                            path+=(bound[index_c-1]+index_p+1)+" ";
                        }
                    }
                    bw.write(path);
                    bw.newLine();
                }
            }
            bw.close();

            //Output info
            bw = new BufferedWriter(new FileWriter("output\\"+filename+"_info.txt"));

            bw.write("Info");
            bw.newLine();
            bw.write("--");
            bw.newLine();
            bw.write("User: "+total_user);
            bw.newLine();
            bw.write("Session: "+total_session);
            bw.newLine();
            bw.write("Avg.Length: "+Math.round((avg_length/(double)total_session)*100.0)/100.0);
            bw.newLine();
            bw.write("Category: "+total_category);
            bw.newLine();
            bw.write("Page: "+total_page);
            bw.newLine();
            bw.newLine();
            bw.write("Category");//Information of category
            bw.newLine();
            bw.write("--");
            bw.newLine();
            for(int a=0;a<categories.size();a++){
                bw.write((a+1)+", "+categories.get(a).category);
                bw.newLine();
            }
            bw.newLine();
            bw.write("Page");//Information of page
            bw.newLine();
            bw.write("--");
            bw.newLine();
            int counter=0;
            for(int a=0;a<categories.size();a++){
                for(int b=0;b<categories.get(a).pages.size();b++){
                    bw.write((counter+1)+", "+categories.get(a).pages.get(b));
                    bw.newLine();
                    counter++;
                }
            }
            bw.close();

            //Output bound
            bw = new BufferedWriter(new FileWriter("output\\"+filename+"_bound.txt"));

            for(int a=0;a<bound.length;a++){
                bw.write(String.valueOf(bound[a]));
                bw.newLine();
            }
            bw.close();
        }
        catch(Exception e){
            System.out.println(e);
            System.out.println("DataPreprocessing-PathCompletion:¨Ò¥~¿ù»~!!");
        }
    }

    public static void main(String args[]){
        //DataPreprocessing dp = new DataPreprocessing(args[0],args[1],args[2],args[3]);
        DataPreprocessing dp = new DataPreprocessing("nasa_50000.txt","0","30","1000");//File,level,time out,hash_table_size
    }
}