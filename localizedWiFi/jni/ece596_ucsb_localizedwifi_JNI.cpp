#include "ece596_ucsb_localizedwifi_JNI.h"
#include<android/log.h>
#include<iostream>
#include<vector>
#include<map>
#include<fstream>
#include<sstream>
#include<string>
#include<stdio.h>
#include <cstdlib>
#include <locale.h>
#include <list> 
#include<cstdlib>
#include<string.h>
#include <math.h>
#include<algorithm>
using namespace std;
#define LOG_TAG "jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

class loc_info{
	public:
		string ap_name;
		double rssi_avg,sum,sjf;
		map<double, double, greater<double> > rssi;
	loc_info(){
		ap_name="";
		rssi_avg=0;
		sum=0;
		sjf=0;
	}
	bool operator == (const loc_info &temp){
        return (ap_name == temp.ap_name);
    }
};
class pair_match{
	public:
	int a;
	double b;	
	pair_match(){
		a=0;
		b=0;
	}
	bool operator == (const pair_match &temp){
        return (a == temp.a);
    }
};

//sort in descending order//
bool cmp(const loc_info & v1,const loc_info & v2){
	return (v1.rssi_avg>v2.rssi_avg);
}
bool my(const loc_info & v1,const loc_info & v2){
	if (v1.sjf!=v2.sjf)	return (v1.sjf<v2.sjf);
	else return (v1.rssi_avg>v2.rssi_avg);
}
bool rank_pr(const pair_match & p1,const pair_match & p2){
	return (p1.b>p2.b);
}

bool pred(const pair<double, double>& lhs, const pair<double, double>& rhs)
{
return lhs.second < rhs.second;
}
void read_offline(vector< map<string, vector<double> > > &matrix1){
	ifstream is;
	string str="",bssid="";
	int num=0,rssi=0;
	is.open("/mnt/sdcard/database.txt");
	map< string, vector<double> >::iterator iter;
	string line="";
	while(!is.eof()){
		getline(is,line);
		if(line.empty()) break;
		istringstream stream(line);
		while(stream.good()){
			stream>>str;
			//Get the location num//
			if(str.compare(0,11,"LocationNum",0,11)==0){
				stream>>str;
				stream>>num;
				num--;
			}
			//get the pair of BSSID and RSSI//
			if(str.compare(0,3,"UCSB",0,3)==0 || str.compare(0,9,"Intermezzo",0,9)==0 ){
				if(str.compare(0,3,"UCSB",0,3)==0){
					for(int i=0;i<4;i++){
						stream>>bssid;
					}
				}
				else{
					for(int i=0;i<2;i++){
						stream>>bssid;
					}
				}
				bssid=bssid.substr(0,17);
				while(str!="level:"){
					stream>>str;
				}
				stream>>rssi;
				if( matrix1[num].find(bssid)== (matrix1[num]).end() ){
					matrix1[num].insert(make_pair(bssid,0));
					iter=matrix1[num].find(bssid);
					(*iter).second.push_back(rssi);
				}
				else{
					iter=matrix1[num].find(bssid);
					((*iter).second).push_back(rssi);
				}
			}
		}
	}
	is.close();
return;
}

void read(vector< map<string, vector<double> > > &matrix1,vector< vector<loc_info> > &matrix2){
	map< string, vector<double> >::iterator iter1;
	vector<double>::iterator iter2;
	double pr=0,sum=0;
	for(int i=0;i<matrix1.size();i++){
		for( iter1=(matrix1[i]).begin(); iter1!=(matrix1[i]).end(); iter1++){
			loc_info dummy;
			dummy.ap_name=(*iter1).first;
			for( iter2= ((*iter1).second).begin();iter2!= ((*iter1).second).end();iter2++){
				pr=count(  ((*iter1).second).begin(),((*iter1).second).end(),(*iter2) ) / double(((*iter1).second).size());
				(dummy.rssi).insert( make_pair( (*iter2),pr ) ) ;
				sum+=(*iter2);
			}
			dummy.sum=sum;
			dummy.rssi_avg=sum/(((*iter1).second).size());
			sum=0;
			matrix2[i].push_back(dummy);
		}
		sort(matrix2[i].begin(),matrix2[i].end(),cmp);
	}
	return;
}

void read_online(vector<loc_info> &test){
	ifstream is;
	vector<loc_info>::iterator iter;
	map<double, double, greater<double> >::iterator iter1;
	string str="",bssid="";
	int rssi=0;
	is.open("/mnt/sdcard/log.file");//real time data file//
	string line="";
	while(!is.eof()){
		getline(is,line);
		if(line.empty()) break;
		istringstream stream(line);
		while(stream.good()){
			stream>>str;
			//get the pair of BSSID and RSSI//
			if(str.compare(0,3,"UCSB",0,3)==0 || str.compare(0,9,"Intermezzo",0,9)==0 ){
				if(str.compare(0,3,"UCSB",0,3)==0){
					for(int i=0;i<4;i++){
						stream>>bssid;
					}
				}
				else{
					for(int i=0;i<2;i++){
						stream>>bssid;
					}
				}
				bssid=bssid.substr(0,17);
				loc_info testing;
				testing.ap_name=bssid;
				iter=find(test.begin(),test.end(),testing);
				if(iter==test.end()){
					loc_info l;
					l.ap_name=bssid;
					while(str!="level:"){
						stream>>str;
					}
					stream>>rssi;
					(l.rssi).insert( make_pair(rssi,1));//map<double,double>=map<rssi,occurence>
					test.push_back(l);
				}
				else{
					while(str!="level:"){
						stream>>str;
					}
					stream>>rssi;
					iter1=((*iter).rssi).find(rssi);
					if(iter1!=((*iter).rssi).end())	(*iter1).second=((*iter1).second)+1;
					else ((*iter).rssi).insert( make_pair(rssi,1) );
				}
			}
		}
	}
	is.close();
	//calculate the average value of samples at the same location//
	double sum=0,count=0;
	for(int i=0;i<test.size();i++){
		for(iter1=(test[i].rssi).begin();iter1!=(test[i].rssi).end();iter1++){
			sum+=((*iter1).first)*((*iter1).second);
			count+=(*iter1).second;
		}

		//test[i].sjf=-test[i].sjf/max_element(test[i].rssi.begin(), test[i].rssi.end(), pred)->second;
		test[i].rssi_avg=int(sum/count);
		for(iter1=(test[i].rssi).begin();iter1!=(test[i].rssi).end();iter1++){
			test[i].sjf+=((*iter1).first-test[i].rssi_avg)*((*iter1).first-test[i].rssi_avg)*( (*iter1).second);
		}
		test[i].sjf/=(test[i].rssi).size();
		//test[i].sjf=abs((max_element(test[i].rssi.begin(), test[i].rssi.end(), pred)->second)-(min_element(test[i].rssi.begin(), test[i].rssi.end(), pred)->second));
		//test[i].sjf*=abs(test[i].rssi_avg);
		cout<<test[i].rssi_avg<<endl;
		sum=0;
		count=0;
	}
	sort(test.begin(),test.end(),my);
return;	
}

JNIEXPORT jintArray JNICALL Java_ece596_ucsb_localizedwifi_JNI_getCInt(JNIEnv *env, jobject thiz){
	LOGI("welcome");
	//initialization//
		vector< map<string, vector<double> > > matrix1;//initial offline database location(ap_name(rssi))
		vector< vector<loc_info> > matrix2;//offline database location( class loc_info(rssi map, rssi_avg) )
		vector<loc_info> input;//real data
		matrix1.resize(30);
		matrix2.resize(30);

		vector<loc_info>::iterator iter1;//for input
		vector<loc_info>::iterator iter2;//for database
		map<double, double, greater<double> >::iterator iter3;// for search
		vector< map<string, vector<double> > >::iterator iter;
		//main function//
		read_offline(matrix1);//read the offline data
		LOGI("matrix1");
		read(matrix1,matrix2);//construct the distriution structure
		LOGI("matrix2");
		read_online(input);//read the real-time data
		LOGI("real-time data");
		vector< pair_match> match;
		vector< pair_match>::iterator iter4;
		double in_rssi=0,pr=0,previous=0,threshold=0.2;
		int window=(matrix2.size()+1)/3,count=0;;
		string in_ap_name="";
		iter1=input.begin();
		in_rssi=(*iter1).rssi_avg;
		in_ap_name=(*iter1).ap_name;

		//make multimap<pr,locationNum>//
		for(int i=0;i<matrix2.size();i++){
			pair_match temp;
			iter2=find(matrix2[i].begin(),matrix2[i].end(),(*iter1));
			if (iter2==matrix2[i].end()){
				temp.a=i;
				temp.b=0;
				match.push_back(temp);
			}
			else{
				iter3=((*iter2).rssi).find(in_rssi);
				if(iter3==((*iter2).rssi).end()){
					temp.a=i;
					temp.b=0;
					match.push_back(temp);
				}
				else{
					temp.a=i;
					temp.b=(*iter3).second;
					match.push_back(temp);
				}
			}
		}
		sort(match.begin(),	match.end(),rank_pr);

		for(iter4=match.begin();iter4!=match.end();iter4++){
			if(count==window-1){
				match.erase(iter4,match.end());
				break;
			}
			count++;
		}
		count=0;
		for(iter1=input.begin()+1;iter1!=input.end();iter1++){
			in_rssi=(*iter1).rssi_avg;
			in_ap_name=(*iter1).ap_name;
			for(iter4=match.begin();iter4!=match.end();iter4++){
				iter2=find(matrix2[(*iter4).a].begin(),matrix2[(*iter4).a].end(),(*iter1));
				if(iter2==matrix2[(*iter4).a].end()) (*iter4).b=0;
				else{
					iter3=((*iter2).rssi).find(in_rssi);
					if(iter3!=((*iter2).rssi).end()){
						(*iter4).b=(*iter3).second;
					}
					else (*iter4).b=0;
				}
			}
			sort(match.begin(),	match.end(),rank_pr);
			for(iter4=match.begin();iter4!=match.end();iter4++){
				if(iter4!=match.begin()){
					if(previous-(*iter4).b > threshold){
						match.erase(iter4,match.end());
						break;
					}
				}
				previous=(*iter4).b;
				count++;
			}
			count=0;
			if (match.size()==1){
				iter4=match.begin();
				//cout<<"the location is:"<<(*iter4).a+1<<endl;
				//return ((*iter4).a+1);

			}

		}
		//cout<<endl;
		sort(match.begin(),	match.end(),rank_pr);
		int sjf=0;//sjf is the number of top candidate you want
		if (match.size()!=1){
			sort(match.begin(),	match.end(),rank_pr);
			for(iter4=match.begin();iter4!=match.end();iter4++){
				if(sjf==5){
					match.erase(iter4,match.end());
					break;
				}
				sjf++;
			}
		}
		sjf=0;
		//if (match.size()==0) return null;
		//cout<<"The final compare result is:"<<endl;
		jintArray result = env->NewIntArray(5);
		jint fill[5];
		int i=0;
		for(iter4=match.begin();iter4!=match.end();iter4++){
			fill[i]=((*iter4).a+1);
			i++;
			//cout<<"the location is: "<<(*iter4).a+1<<"  Pr is:"<<(*iter4).b<<endl;
			//return ((*iter4).a+1);
		}
		env->SetIntArrayRegion(result, 0, i, fill);
		return result;

}

JNIEXPORT jstring JNICALL Java_ece596_ucsb_localizedwifi_JNI_getCString(JNIEnv *env, jobject thiz){

    return env->NewStringUTF( "Your Location is: " );

}
