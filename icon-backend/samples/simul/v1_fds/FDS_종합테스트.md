# 감지 시나리오
S_EDLY_ND_HI
SCN_AUTH_XFER
S_R005
S_CUS014
SCN_DORM_ACC


DS_CRM
```json
[
  {"customer_id":"DEMO_CUS001","customer_age":68,"grade":"VIP","region":"서울","join_date":"2020-03-15"}
  , {"customer_id":"DEMO_CUS888","customer_age":42,"grade":"일반","region":"서울","join_date":"2023-06-15"}
  , {"customer_id":"DEMO_CUS999","customer_age":35,"grade":"일반","region":"서울","join_date":"2024-05-10"}
  , {"customer_id":"DEMO_CUS100","customer_age":35,"grade":"우수","region":"서울","join_date":"2020-03-15"}
  , {"customer_id":"DEMO_CUS200","customer_age":42,"grade":"일반","region":"경기","join_date":"2019-07-10"}
  , {"customer_id":"DEMO_CUS300","customer_age":28,"grade":"일반","region":"인천","join_date":"2021-11-05"}
  , {"customer_id":"DEMO_CUS400","customer_age":51,"grade":"일반","region":"부산","join_date":"2018-02-20"}
  , {"customer_id":"DEMO_CUS555","customer_age":45,"grade":"일반","region":"서울","join_date":"2022-01-15"}
  , {"customer_id":"DEMO_CUS777","customer_age":52,"grade":"일반","region":"부산","join_date":"2021-06-20"}
]
```

DS_ACC_OPEN
```json
[
  {"account_number":"110-123-456789","customer_id":"DEMO_CUS001","account_open_date":"2025-12-02 15:00:00","account_type":"비대면계좌","transaction_type":"비대면계좌개설","initial_balance":50000000}
  , {"account_number":"110-999-888777","customer_id":"DEMO_CUS999","account_open_date":"2024-06-01 10:00:00","account_type":"대면계좌","transaction_type":"대면계좌개설","initial_balance":10000000}
  , {"account_number":"110-100-111111","customer_id":"DEMO_CUS100","account_open_date":"2020-03-20 10:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-12-10 08:00:00"}
  , {"account_number":"220-200-222222","customer_id":"DEMO_CUS200","account_open_date":"2019-07-15 09:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-12-09 15:30:00"}
  , {"account_number":"330-300-333333","customer_id":"DEMO_CUS300","account_open_date":"2021-11-10 11:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-12-08 12:45:00"}
  , {"account_number":"440-400-444444","customer_id":"DEMO_CUS400","account_open_date":"2018-02-25 14:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-12-07 10:20:00"}
  , {"account_number":"110-555-111111","customer_id":"DEMO_CUS555","account_open_date":"2022-01-20 10:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-11-20 14:30:00"}
  , {"account_number":"220-777-999999","customer_id":"DEMO_CUS777","account_open_date":"2021-07-01 09:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2024-10-15 11:20:00"}
]
```

DS_AUTH
```json
[
  {"auth_id":"AUTH_888_20251204_100000","customer_id":"DEMO_CUS888","auth_type":"OTP","auth_issued_at":"2025-12-04 10:00:00","auth_method":"SMS","auth_status":"SUCCESS"}
]
```

0ME2YHMK6FGZN: 금융거래
```json
[
  {"CUS_ID":"DEMO_CUS001","TRX_DT":"2025-12-03 09:30:00","TRX_AMT":7000000,"TRX_TYPE":"이체","BAL_AMT":10000000,"BALANCE_BEFORE":25000000,"SENDER":"110-123-456789","RECEIVER":"990-333-444444","LOGIN_FAIL_CNT":0,"NON_FACE_YN":"Y","ACCESS_COUNTRY":"KR","ACCESS_IP":"192.168.1.100","DEVICE_ID":"DEVICE_DEMO001","ATM_WD_CNT":0,"IS_NIGHT_TIME":"N"}
  , {"CUS_ID":"DEMO_CUS888","TRX_DT":"2025-12-04 12:00:00","TRX_AMT":3000000,"TRX_TYPE":"이체","BAL_AMT":2000000,"SENDER":"110-987-654321","RECEIVER":"990-111-222333"}
  , {"CUS_ID":"DEMO_CUS999","TRX_DT":"2025-12-04 23:30:00","TRX_AMT":1000000,"TRX_TYPE":"출금","BAL_AMT":9000000,"ATM_WD_CNT":1}
  , {"CUS_ID":"DEMO_CUS100","TRX_DT":"2025-12-10 09:00:00","TRX_AMT":2000000,"TRX_TYPE":"이체","BAL_AMT":8000000,"SENDER":"110-100-111111","RECEIVER":"220-200-222222"}
  , {"CUS_ID":"DEMO_CUS100","TRX_DT":"2025-12-10 09:20:00","TRX_AMT":1500000,"TRX_TYPE":"이체","BAL_AMT":6500000,"SENDER":"110-100-111111","RECEIVER":"330-300-333333"}
  , {"CUS_ID":"DEMO_CUS100","TRX_DT":"2025-12-10 09:40:00","TRX_AMT":3000000,"TRX_TYPE":"이체","BAL_AMT":3500000,"SENDER":"110-100-111111","RECEIVER":"440-400-444444"}
  , {"CUS_ID":"DEMO_CUS555","TRX_DT":"2025-12-02 09:00:00","TRX_AMT":5000000,"TRX_TYPE":"이체","BAL_AMT":5000000,"SENDER":"110-555-111111","RECEIVER":"220-777-999999"}
]
```

