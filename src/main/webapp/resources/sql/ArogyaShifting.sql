alter table bill modify invoicedate datetime null;
alter table bill add smsedat datetime null;
alter table bill add vat double null;
alter table bill add vatPlusNetTotal double null;
alter table bill add SMSEDUSER_ID long null;
alter table billitem modify totime datetime null;
alter table billitem add vat double null;
alter table payment modify paymentmethod varchar(255) null;
