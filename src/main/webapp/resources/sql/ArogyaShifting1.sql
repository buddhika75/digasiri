alter table billsession drop foreign key FK_BILLSESSION_BILLITEM_ID;
alter table billsession drop foreign key FK_BILLSESSION_BILL_ID;
alter table bill drop foreign key FK_BILL_SINGLEBILLITEM_ID;

