UPDATE bill
SET `RETIRED` = true, `RETIRECOMMENTS` = 'Before Delete on 2017 09 05'
 where billdate < '2017-06-01' and retired=false
 and `BILLTYPE` in ('ChannelOnCall','ChannelPaid','ChannelCash','ChannelAgent','ChannelOnCall') 
limit 10000000
;


-- 
-- select id, billdate, billtype, `BILLTOTAL`,`RETIRED`,`RETIRECOMMENTS` from bill 
-- where billdate < '2017-06-01' and `RETIRED`=true and `RETIRECOMMENTS` is not null
-- and `BILLTYPE` in ('ChannelOnCall','ChannelPaid','ChannelCash','ChannelAgent','ChannelOnCall') 
-- order by id desc
-- limit 10;

-- UPDATE bill
-- SET `RETIRED` = true, `RETIRECOMMENTS` = 'Before Delete on 2017 09 05'
-- WHERE `RETIRED` = false and `BILLDATE` < #2017 05 31#