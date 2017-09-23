alter table bill modify invoicedate datetime null;
alter table bill add smsedat datetime null;
alter table bill add vat double null;
alter table bill add vatPlusNetTotal double null;
alter table bill add SMSEDUSER_ID long null;