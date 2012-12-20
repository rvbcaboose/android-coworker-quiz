/**
 * Apex REST class to save a score for a user. Currently called from
 * Coworker-quiz android native application.
 *
 * @author: appirio-appdev
 * @date: 12/20/2011
 */
@RestResource(urlMapping='/CoWorkerQuiz/*')
global without sharing class CoWorkerQuizREST {
    @HttpPost 
    global static String saveScore(RestRequest req, RestResponse res, 
                                   Integer lowScore, Integer highScore) {
        CQ_LeaderBoard__c userStat = null;
        boolean need_upsert = false;
        String userId = UserInfo.getUserId();
        List <CQ_LeaderBoard__c> userStats = [SELECT User_Id__c, Name, Low_Score__c, High_Score__c FROM CQ_LeaderBoard__c WHERE User_Id__c=:userId];
        if(userStats.size() > 0) {
            userStat = userStats[0];     
            if(lowScore > userStat.Low_Score__c) { // update only if current low is higher
                need_upsert = true;
                userStat.Low_Score__c = lowScore;
            }
            if(highScore > userStat.High_Score__c) { // update only if current high is higher
                need_upsert = true;
                userStat.High_Score__c = highScore;
            }
        }
        else {  // stats never entered before.  Create record.
            need_upsert = true;
            String name = UserInfo.getFirstName() + ' ' + UserInfo.getLastName();
            userStat = new CQ_LeaderBoard__c();
            userStat.name = name;
            userStat.User_Id__c = userId;
            userStat.Low_Score__c = lowScore;
            userStat.High_Score__c = highScore;
        }
        
        String status = 'NO_UPDATE';
        if(need_upsert == true && null != userStat) {
            upsert userStat;
            status = 'UPDATE';
        }  
        return status;
    }
}