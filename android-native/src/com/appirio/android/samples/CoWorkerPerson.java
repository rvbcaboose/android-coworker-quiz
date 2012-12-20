package com.appirio.android.samples;

public class CoWorkerPerson {
    private String id;
    private String fName;
    private String lName;
    private String picUrl;

    public boolean equals(Object that) {
        if(null != that && that instanceof CoWorkerPerson) {
            return getName().equals(((CoWorkerPerson)that).getName());
        }
        return false;
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public String getId() {
        return id;
    }
	public String getName(){
	  return fName + " " + lName;
	}
	
	public String getFName(){
	  return fName;
	}

    public void setId(String id) {
        this.id = id;
    }

	public void setFName(String pFName){
	  fName = pFName;
	}
	public String getLName(){
	  return lName;
	}
	public void setLName(String pLName){
	  lName = pLName;
	}
	public String getPicUrl(){
	  return picUrl;
	}
	public void setPicUrl(String pPicUrl){
	  picUrl = pPicUrl;
	}
}
