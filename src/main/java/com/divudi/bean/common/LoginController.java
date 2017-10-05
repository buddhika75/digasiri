/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Logins;
import com.divudi.facade.LoginsFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named(value = "loginController")
@SessionScoped
public class LoginController implements Serializable {

    Department department;
    Institution institution;
    Date fromDate;
    Date toDate;
    Logins longin;
    List<Logins> logins;
    @Inject
    LoginsFacade facade;

    /**
     * Creates a new instance of LoginController
     */
    public LoginController() {
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public LoginsFacade getFacade() {
        return facade;
    }

    public void setFacade(LoginsFacade facade) {
        this.facade = facade;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Logins getLongin() {
        return longin;
    }

    public void setLongin(Logins longin) {
        this.longin = longin;
    }

    public String toManageUsers(){
        fillLoginsLastTen();
        return "/admin_manage_users";
    }
    
    public void fillLogins() {
        String sql;
        Map m = new HashMap();
        m.put("fromDate", fromDate);
        m.put("toDate", toDate);
        sql = "select l from Logins l where l.logedAt between :fromDate and :toDate or l.logoutAt between :fromDate and :toDate  order by l.logedAt, l.logoutAt";
        logins = getFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
    }
    
    public String fillLoginsLastTen() {
        String sql;
        Map m = new HashMap();
        sql = "select l from Logins l order by l.logedAt desc";
        logins = getFacade().findBySQL(sql, m, TemporalType.TIMESTAMP,10);
        return "/admin/users/index";
    }

    public List<Logins> getLogins() {
        return logins;
    }

    public void setLogins(List<Logins> logins) {
        this.logins = logins;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }
}
