# FII/DII/OI Upstox probe (2026-09-19)

Probe OK — auto-continue to backend. Default OI expiry: `current_month` (`current_week` returned empty data).

## Instrument key map
- `NIFTY 50` -> `NSE_INDEX|Nifty 50`
- `NIFTY BANK` -> `NSE_INDEX|Nifty Bank`

## fii-1D
- HTTP 200, bytes=30569
- data keys: NSE_FO|INDEX_OPTIONS, NSE_FO|INDEX_FUTURES, NSE_EQ|CASH
```json
{"status":"success","data":{"NSE_FO|INDEX_OPTIONS":[{"time_stamp":1789669800000,"buy_amount":833775.01,"sell_amount":829370.24,"buy_contracts":5451316,"sell_contracts":5419083,"oi_contracts":3618073,"oi_amount":559755.87,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":690977,"total_put_long_contracts":1299085,"total_call_short_contracts":959379,"total_put_short_contracts":668632},{"time_stamp":1789583400000,"buy_amount":790447.63,"sell_amount":784915.27,"buy_contracts":5165560,"sell_contracts":5127698,"oi_contracts":3335095,"oi_amount":514885.94,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":645825,"total_put_long_contracts":1186632,"total_call_short_contracts":928717,"total_put_short_contracts":573922},{"time_stamp":1789497000000,"buy_amount":697114.76,"sell_amount":708600.26,"buy_contracts":4549819,"sell_contracts":4623342,"oi_contracts":3179009,"oi_amount":490368.86,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":598953,"total_put_long_contracts":1136529,"total_call_short_contracts":887159,"total_put_short_contracts":556367},{"time_stamp":1789410600000,"buy_amount":2733979.49,"sell_amount":2753830.77,"buy_contracts":17921112,"sell_contracts":18071735,"oi_contracts":2807932,"oi_amount":432073.43,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":528339,"total_put_long_contracts":1058367,"total_call_short_contracts":841380,"total_put_short_contracts":379847},{"time_stamp":1789065000000,"buy_amount":1462020.76,"sell_amount":1457585.46,"buy_contracts":9557182,"sell_contracts":9518462,"oi_contracts":3632183,"oi_amount":562259.32,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":675792,"total_put_long_contracts":1331606,"tot
...TRUNC
```

## dii-1D
- HTTP 200, bytes=9492
- data keys: NSE_EQ|CASH
```json
{"status":"success","data":{"NSE_EQ|CASH":[{"time_stamp":1789669800000,"buy_amount":17310.04,"sell_amount":16290.35,"buy_contracts":0,"sell_contracts":0,"oi_contracts":0,"oi_amount":0.0,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":0,"total_put_long_contracts":0,"total_call_short_contracts":0,"total_put_short_contracts":0},{"time_stamp":1789583400000,"buy_amount":14105.01,"sell_amount":10487.26,"buy_contracts":0,"sell_contracts":0,"oi_contracts":0,"oi_amount":0.0,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":0,"total_put_long_contracts":0,"total_call_short_contracts":0,"total_put_short_contracts":0},{"time_stamp":1789497000000,"buy_amount":15279.59,"sell_amount":11371.36,"buy_contracts":0,"sell_contracts":0,"oi_contracts":0,"oi_amount":0.0,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":0,"total_put_long_contracts":0,"total_call_short_contracts":0,"total_put_short_contracts":0},{"time_stamp":1789410600000,"buy_amount":15221.98,"sell_amount":12535.93,"buy_contracts":0,"sell_contracts":0,"oi_contracts":0,"oi_amount":0.0,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":0,"total_put_long_contracts":0,"total_call_short_contracts":0,"total_put_short_contracts":0},{"time_stamp":1789065000000,"buy_amount":15109.58,"sell_amount":13141.41,"buy_contracts":0,"sell_contracts":0,"oi_contracts":0,"oi_amount":0.0,"total_long_contracts":0,"total_short_contracts":0,"total_call_long_contracts":0,"total_put_long_contracts":0,"total_call_short_contracts":0,"total_put_short_contracts":0},{"time_stamp":1788978600000,"buy_amount":13326.33,"sell_amount":12300.48,"buy_contracts":0,"sell_contracts":0,"oi_contracts":0,"oi_amount":0.0,"total_long_contracts":0,"total_short_c
...TRUNC
```

## oi-nifty-week
- HTTP 200, bytes=32
```json
{"status":"success","data":null}
```

## oi-nifty-month
- HTTP 200, bytes=7312
- data keys: total_puts, total_calls, spot_closing_price, expiry, call_put_oi_data_list
```json
{"status":"success","data":{"total_puts":92818980,"total_calls":94165000,"spot_closing_price":23346.4,"expiry":"29-09-2026","call_put_oi_data_list":[{"call_oi":0,"put_oi":94445,"strike_price":15000.0},{"call_oi":0,"put_oi":130455,"strike_price":16500.0},{"call_oi":910,"put_oi":814970,"strike_price":18000.0},{"call_oi":140465,"put_oi":1029275,"strike_price":19500.0},{"call_oi":510055,"put_oi":3692260,"strike_price":21000.0},{"call_oi":0,"put_oi":66560,"strike_price":21350.0},{"call_oi":0,"put_oi":203775,"strike_price":21400.0},{"call_oi":0,"put_oi":8190,"strike_price":21450.0},{"call_oi":0,"put_oi":1078805,"strike_price":21500.0},{"call_oi":0,"put_oi":10075,"strike_price":21550.0},{"call_oi":46410,"put_oi":952705,"strike_price":21600.0},{"call_oi":65,"put_oi":46735,"strike_price":21650.0},{"call_oi":65,"put_oi":657540,"strike_price":21700.0},{"call_oi":65,"put_oi":40755,"strike_price":21750.0},{"call_oi":65,"put_oi":942565,"strike_price":21800.0},{"call_oi":65,"put_oi":50960,"strike_price":21850.0},{"call_oi":585,"put_oi":367120,"strike_price":21900.0},{"call_oi":130,"put_oi":185965,"strike_price":21950.0},{"call_oi":784095,"put_oi":4009980,"strike_price":22000.0},{"call_oi":65,"put_oi":180635,"strike_price":22050.0},{"call_oi":1040,"put_oi":410800,"strike_price":22100.0},{"call_oi":390,"put_oi":134680,"strike_price":22150.0},{"call_oi":1300,"put_oi":1129765,"strike_price":22200.0},{"call_oi":325,"put_oi":244335,"strike_price":22250.0},{"call_oi":1495,"put_oi":781365,"strike_price":22300.0},{"call_oi":130,"put_oi":163540,"strike_price":22350.0},{"call_oi":520,"put_oi":1464125,"strike_price":22400.0},{"call_oi":65,"put_oi":283855,"strike_price":22450.0},{"call_oi":590655,"put_oi":4601870,"strike_price":22500.0},{"call_oi":520,"put_oi":337870,"strike_price":22550.0},{"call
...TRUNC
```

## coi-nifty-month-1
- HTTP 200, bytes=8863
- data keys: total_put_change_oi, total_call_change_oi, spot_closing_price, expiry, call_put_oi_data_list
```json
{"status":"success","data":{"total_put_change_oi":7654960,"total_call_change_oi":5525845,"spot_closing_price":23346.4,"expiry":"29-09-2026","call_put_oi_data_list":[{"strike_price":15000.0,"call_change_oi":0,"put_change_oi":1690},{"strike_price":16500.0,"call_change_oi":0,"put_change_oi":-65},{"strike_price":18000.0,"call_change_oi":0,"put_change_oi":-4225},{"strike_price":19500.0,"call_change_oi":0,"put_change_oi":3770},{"strike_price":21000.0,"call_change_oi":-4095,"put_change_oi":752050},{"strike_price":21350.0,"call_change_oi":0,"put_change_oi":17095},{"strike_price":21400.0,"call_change_oi":0,"put_change_oi":103090},{"strike_price":21450.0,"call_change_oi":0,"put_change_oi":3315},{"strike_price":21500.0,"call_change_oi":0,"put_change_oi":582010},{"strike_price":21550.0,"call_change_oi":0,"put_change_oi":1950},{"strike_price":21600.0,"call_change_oi":-260,"put_change_oi":77870},{"strike_price":21650.0,"call_change_oi":0,"put_change_oi":-17225},{"strike_price":21700.0,"call_change_oi":0,"put_change_oi":70395},{"strike_price":21750.0,"call_change_oi":0,"put_change_oi":6110},{"strike_price":21800.0,"call_change_oi":0,"put_change_oi":4290},{"strike_price":21850.0,"call_change_oi":0,"put_change_oi":-18785},{"strike_price":21900.0,"call_change_oi":0,"put_change_oi":32500},{"strike_price":21950.0,"call_change_oi":0,"put_change_oi":-13260},{"strike_price":22000.0,"call_change_oi":-13000,"put_change_oi":197665},{"strike_price":22050.0,"call_change_oi":0,"put_change_oi":-17160},{"strike_price":22100.0,"call_change_oi":0,"put_change_oi":69030},{"strike_price":22150.0,"call_change_oi":0,"put_change_oi":67535},{"strike_price":22200.0,"call_change_oi":0,"put_change_oi":452075},{"strike_price":22250.0,"call_change_oi":0,"put_change_oi":130585},{"strike_price":22300.0,"call_change_
...TRUNC
```

