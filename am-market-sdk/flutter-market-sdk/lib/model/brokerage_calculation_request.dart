// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class BrokerageCalculationRequest {
  /// Returns a new [BrokerageCalculationRequest] instance.
  BrokerageCalculationRequest({
    this.tradingSymbol,
    this.quantity,
    this.buyPrice,
    this.sellPrice,
    this.exchange,
    this.tradeType,
    this.brokerType,
    this.brokerName,
    this.brokerFlatFee,
    this.brokerPercentageFee,
    this.stateCode,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? tradingSymbol;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? quantity;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? buyPrice;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? sellPrice;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? exchange;

  BrokerageCalculationRequestTradeTypeEnum? tradeType;

  BrokerageCalculationRequestBrokerTypeEnum? brokerType;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? brokerName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? brokerFlatFee;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? brokerPercentageFee;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? stateCode;

  @override
  bool operator ==(Object other) => identical(this, other) || other is BrokerageCalculationRequest &&
    other.tradingSymbol == tradingSymbol &&
    other.quantity == quantity &&
    other.buyPrice == buyPrice &&
    other.sellPrice == sellPrice &&
    other.exchange == exchange &&
    other.tradeType == tradeType &&
    other.brokerType == brokerType &&
    other.brokerName == brokerName &&
    other.brokerFlatFee == brokerFlatFee &&
    other.brokerPercentageFee == brokerPercentageFee &&
    other.stateCode == stateCode;

  @override
  int get hashCode =>
    (tradingSymbol == null ? 0 : tradingSymbol!.hashCode) +
    (quantity == null ? 0 : quantity!.hashCode) +
    (buyPrice == null ? 0 : buyPrice!.hashCode) +
    (sellPrice == null ? 0 : sellPrice!.hashCode) +
    (exchange == null ? 0 : exchange!.hashCode) +
    (tradeType == null ? 0 : tradeType!.hashCode) +
    (brokerType == null ? 0 : brokerType!.hashCode) +
    (brokerName == null ? 0 : brokerName!.hashCode) +
    (brokerFlatFee == null ? 0 : brokerFlatFee!.hashCode) +
    (brokerPercentageFee == null ? 0 : brokerPercentageFee!.hashCode) +
    (stateCode == null ? 0 : stateCode!.hashCode);

  @override
  String toString() => 'BrokerageCalculationRequest[tradingSymbol=$tradingSymbol, quantity=$quantity, buyPrice=$buyPrice, sellPrice=$sellPrice, exchange=$exchange, tradeType=$tradeType, brokerType=$brokerType, brokerName=$brokerName, brokerFlatFee=$brokerFlatFee, brokerPercentageFee=$brokerPercentageFee, stateCode=$stateCode]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.tradingSymbol != null) {
      json[r'tradingSymbol'] = this.tradingSymbol;
    } else {
      json[r'tradingSymbol'] = null;
    }
    if (this.quantity != null) {
      json[r'quantity'] = this.quantity;
    } else {
      json[r'quantity'] = null;
    }
    if (this.buyPrice != null) {
      json[r'buyPrice'] = this.buyPrice;
    } else {
      json[r'buyPrice'] = null;
    }
    if (this.sellPrice != null) {
      json[r'sellPrice'] = this.sellPrice;
    } else {
      json[r'sellPrice'] = null;
    }
    if (this.exchange != null) {
      json[r'exchange'] = this.exchange;
    } else {
      json[r'exchange'] = null;
    }
    if (this.tradeType != null) {
      json[r'tradeType'] = this.tradeType;
    } else {
      json[r'tradeType'] = null;
    }
    if (this.brokerType != null) {
      json[r'brokerType'] = this.brokerType;
    } else {
      json[r'brokerType'] = null;
    }
    if (this.brokerName != null) {
      json[r'brokerName'] = this.brokerName;
    } else {
      json[r'brokerName'] = null;
    }
    if (this.brokerFlatFee != null) {
      json[r'brokerFlatFee'] = this.brokerFlatFee;
    } else {
      json[r'brokerFlatFee'] = null;
    }
    if (this.brokerPercentageFee != null) {
      json[r'brokerPercentageFee'] = this.brokerPercentageFee;
    } else {
      json[r'brokerPercentageFee'] = null;
    }
    if (this.stateCode != null) {
      json[r'stateCode'] = this.stateCode;
    } else {
      json[r'stateCode'] = null;
    }
    return json;
  }

  /// Returns a new [BrokerageCalculationRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static BrokerageCalculationRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "BrokerageCalculationRequest[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "BrokerageCalculationRequest[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return BrokerageCalculationRequest(
        tradingSymbol: mapValueOfType<String>(json, r'tradingSymbol'),
        quantity: mapValueOfType<int>(json, r'quantity'),
        buyPrice: num.parse('${json[r'buyPrice']}'),
        sellPrice: num.parse('${json[r'sellPrice']}'),
        exchange: mapValueOfType<String>(json, r'exchange'),
        tradeType: BrokerageCalculationRequestTradeTypeEnum.fromJson(json[r'tradeType']),
        brokerType: BrokerageCalculationRequestBrokerTypeEnum.fromJson(json[r'brokerType']),
        brokerName: mapValueOfType<String>(json, r'brokerName'),
        brokerFlatFee: num.parse('${json[r'brokerFlatFee']}'),
        brokerPercentageFee: num.parse('${json[r'brokerPercentageFee']}'),
        stateCode: mapValueOfType<String>(json, r'stateCode'),
      );
    }
    return null;
  }

  static List<BrokerageCalculationRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <BrokerageCalculationRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = BrokerageCalculationRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, BrokerageCalculationRequest> mapFromJson(dynamic json) {
    final map = <String, BrokerageCalculationRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = BrokerageCalculationRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of BrokerageCalculationRequest-objects as value to a dart map
  static Map<String, List<BrokerageCalculationRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<BrokerageCalculationRequest>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = BrokerageCalculationRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class BrokerageCalculationRequestTradeTypeEnum {
  /// Instantiate a new enum with the provided [value].
  const BrokerageCalculationRequestTradeTypeEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const DELIVERY = BrokerageCalculationRequestTradeTypeEnum._(r'DELIVERY');
  static const INTRADAY = BrokerageCalculationRequestTradeTypeEnum._(r'INTRADAY');

  /// List of all possible values in this [enum][BrokerageCalculationRequestTradeTypeEnum].
  static const values = <BrokerageCalculationRequestTradeTypeEnum>[
    DELIVERY,
    INTRADAY,
  ];

  static BrokerageCalculationRequestTradeTypeEnum? fromJson(dynamic value) => BrokerageCalculationRequestTradeTypeEnumTypeTransformer().decode(value);

  static List<BrokerageCalculationRequestTradeTypeEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <BrokerageCalculationRequestTradeTypeEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = BrokerageCalculationRequestTradeTypeEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [BrokerageCalculationRequestTradeTypeEnum] to String,
/// and [decode] dynamic data back to [BrokerageCalculationRequestTradeTypeEnum].
class BrokerageCalculationRequestTradeTypeEnumTypeTransformer {
  factory BrokerageCalculationRequestTradeTypeEnumTypeTransformer() => _instance ??= const BrokerageCalculationRequestTradeTypeEnumTypeTransformer._();

  const BrokerageCalculationRequestTradeTypeEnumTypeTransformer._();

  String encode(BrokerageCalculationRequestTradeTypeEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a BrokerageCalculationRequestTradeTypeEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  BrokerageCalculationRequestTradeTypeEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data as Object?) {
        case r'DELIVERY': return BrokerageCalculationRequestTradeTypeEnum.DELIVERY;
        case r'INTRADAY': return BrokerageCalculationRequestTradeTypeEnum.INTRADAY;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [BrokerageCalculationRequestTradeTypeEnumTypeTransformer] instance.
  static BrokerageCalculationRequestTradeTypeEnumTypeTransformer? _instance;
}



class BrokerageCalculationRequestBrokerTypeEnum {
  /// Instantiate a new enum with the provided [value].
  const BrokerageCalculationRequestBrokerTypeEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const DISCOUNT = BrokerageCalculationRequestBrokerTypeEnum._(r'DISCOUNT');
  static const FULL_SERVICE = BrokerageCalculationRequestBrokerTypeEnum._(r'FULL_SERVICE');

  /// List of all possible values in this [enum][BrokerageCalculationRequestBrokerTypeEnum].
  static const values = <BrokerageCalculationRequestBrokerTypeEnum>[
    DISCOUNT,
    FULL_SERVICE,
  ];

  static BrokerageCalculationRequestBrokerTypeEnum? fromJson(dynamic value) => BrokerageCalculationRequestBrokerTypeEnumTypeTransformer().decode(value);

  static List<BrokerageCalculationRequestBrokerTypeEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <BrokerageCalculationRequestBrokerTypeEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = BrokerageCalculationRequestBrokerTypeEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [BrokerageCalculationRequestBrokerTypeEnum] to String,
/// and [decode] dynamic data back to [BrokerageCalculationRequestBrokerTypeEnum].
class BrokerageCalculationRequestBrokerTypeEnumTypeTransformer {
  factory BrokerageCalculationRequestBrokerTypeEnumTypeTransformer() => _instance ??= const BrokerageCalculationRequestBrokerTypeEnumTypeTransformer._();

  const BrokerageCalculationRequestBrokerTypeEnumTypeTransformer._();

  String encode(BrokerageCalculationRequestBrokerTypeEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a BrokerageCalculationRequestBrokerTypeEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  BrokerageCalculationRequestBrokerTypeEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data as Object?) {
        case r'DISCOUNT': return BrokerageCalculationRequestBrokerTypeEnum.DISCOUNT;
        case r'FULL_SERVICE': return BrokerageCalculationRequestBrokerTypeEnum.FULL_SERVICE;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [BrokerageCalculationRequestBrokerTypeEnumTypeTransformer] instance.
  static BrokerageCalculationRequestBrokerTypeEnumTypeTransformer? _instance;
}


