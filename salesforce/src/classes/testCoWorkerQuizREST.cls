/**
 * This class contains unit tests for validating the behavior of Apex classes
 * and triggers.
 *
 * Unit tests are class methods that verify whether a particular piece
 * of code is working properly. Unit test methods take no arguments,
 * commit no data to the database, and are flagged with the testMethod
 * keyword in the method definition.
 *
 * All test methods in an organization are executed whenever Apex code is deployed
 * to a production organization to confirm correctness, ensure code
 * coverage, and prevent regressions. All Apex classes are
 * required to have at least 75% code coverage in order to be deployed
 * to a production organization. In addition, all triggers must have some code coverage.
 * 
 * The @isTest class annotation indicates this class only contains test
 * methods. Classes defined with the @isTest annotation do not count against
 * the organization size limit for all Apex scripts.
 *
 * See the Apex Language Reference for more information about Testing and Code Coverage.
 */
@isTest
private class testCoWorkerQuizREST {
    
    private static User createUser() {
        List<User> users = [SELECT Id FROM User where Alias = 'empT'];
        User u;
        if(users.isEmpty()) {
            Profile p = [SELECT Id FROM Profile WHERE Name='System Administrator']; 
            u = new User(Alias = 'empT', Email='employee@testorg.com', 
                          EmailEncodingKey='UTF-8', LastName='Testing', LanguageLocaleKey='en_US', 
                          LocaleSidKey='en_US', ProfileId = p.Id, 
                          TimeZoneSidKey='America/Los_Angeles', UserName='employeeuser@testorg.com');
            insert u;
        }
        else {
            u = users.get(0);
        }
        return u;
    }

    static testMethod void saveScoreExisting() {
        User u = createUser();
        CQ_Leaderboard__c temp = new CQ_Leaderboard__c(Name = 'temp',
                                                       Low_Score__c = 1,
                                                       High_Score__c = 4);
        temp.User_Id__c = u.Id;
        insert temp;    
        System.runAs(u) {
            RestRequest req = new RestRequest();
            RestResponse res = new RestResponse();
            String status = CoWorkerQuizREST.saveScore(req, res, 0, 6);
            temp = [SELECT High_Score__c, Low_Score__c from CQ_Leaderboard__c where User_Id__c = :u.Id];
            System.assertEquals(temp.High_Score__c, 6);
            System.assertEquals(temp.Low_Score__c, 1);
            System.assertEquals(status, 'UPDATE');
            
            status = CoWorkerQuizREST.saveScore(req, res, 5, 5);
            temp = [SELECT High_Score__c, Low_Score__c from CQ_Leaderboard__c where User_Id__c = :u.Id];
            System.assertEquals(temp.High_Score__c, 6);
            System.assertEquals(temp.Low_Score__c, 5);  
            System.assertEquals(status, 'UPDATE');
            
            status = CoWorkerQuizREST.saveScore(req, res, 2, 2);
            temp = [SELECT High_Score__c, Low_Score__c from CQ_Leaderboard__c where User_Id__c = :u.Id];
            System.assertEquals(temp.High_Score__c, 6);
            System.assertEquals(temp.Low_Score__c, 5);  
            System.assertEquals(status, 'NO_UPDATE');
        }
    }
    
    static testMethod void saveScoreNew() {
        User u = createUser();
        System.runAs(u) {
            RestRequest req = new RestRequest();
            RestResponse res = new RestResponse();
            String status = CoWorkerQuizREST.saveScore(req, res, 2, 2);
            
            CQ_Leaderboard__c temp = [SELECT User_Id__c, Low_Score__c, High_Score__c FROM CQ_Leaderboard__c where User_Id__c = :u.Id];
            System.assertNotEquals(temp, null);
            System.assertEquals(temp.User_Id__c, u.Id);
            System.assertEquals(temp.Low_Score__c, 2);
            System.assertEquals(temp.High_Score__c, 2);
            System.assertEquals(status, 'UPDATE');
        }
    }
}