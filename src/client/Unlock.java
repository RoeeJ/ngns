/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.DatabaseConnection;

/**
 *
 * @author Menno
 */
public class Unlock {
    
    protected int id;
    protected int accountId;
    protected String code;
    protected Date unlockedAt;
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }
    
    public int getAccountId() {
        return accountId;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setUnlockedAt(Date unlockedAt) {
        this.unlockedAt = unlockedAt;
    }
    
    public Date getUnlockedAt() {
        return unlockedAt;
    }
}
