// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class Position {
  /// Returns a new [Position] instance.
  Position({
    this.tradingSymbol,
    this.quantity,
    this.type,
    this.product,
    this.exchange,
    this.price,
    this.optionType,
    this.strikePrice,
    this.expiry,
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
  String? type;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? product;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? exchange;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? price;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? optionType;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? strikePrice;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? expiry;

  @override
  bool operator ==(Object other) => identical(this, other) || other is Position &&
    other.tradingSymbol == tradingSymbol &&
    other.quantity == quantity &&
    other.type == type &&
    other.product == product &&
    other.exchange == exchange &&
    other.price == price &&
    other.optionType == optionType &&
    other.strikePrice == strikePrice &&
    other.expiry == expiry;

  @override
  int get hashCode =>
    (tradingSymbol == null ? 0 : tradingSymbol!.hashCode) +
    (quantity == null ? 0 : quantity!.hashCode) +
    (type == null ? 0 : type!.hashCode) +
    (product == null ? 0 : product!.hashCode) +
    (exchange == null ? 0 : exchange!.hashCode) +
    (price == null ? 0 : price!.hashCode) +
    (optionType == null ? 0 : optionType!.hashCode) +
    (strikePrice == null ? 0 : strikePrice!.hashCode) +
    (expiry == null ? 0 : expiry!.hashCode);

  @override
  String toString() => 'Position[tradingSymbol=$tradingSymbol, quantity=$quantity, type=$type, product=$product, exchange=$exchange, price=$price, optionType=$optionType, strikePrice=$strikePrice, expiry=$expiry]';

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
    if (this.type != null) {
      json[r'type'] = this.type;
    } else {
      json[r'type'] = null;
    }
    if (this.product != null) {
      json[r'product'] = this.product;
    } else {
      json[r'product'] = null;
    }
    if (this.exchange != null) {
      json[r'exchange'] = this.exchange;
    } else {
      json[r'exchange'] = null;
    }
    if (this.price != null) {
      json[r'price'] = this.price;
    } else {
      json[r'price'] = null;
    }
    if (this.optionType != null) {
      json[r'optionType'] = this.optionType;
    } else {
      json[r'optionType'] = null;
    }
    if (this.strikePrice != null) {
      json[r'strikePrice'] = this.strikePrice;
    } else {
      json[r'strikePrice'] = null;
    }
    if (this.expiry != null) {
      json[r'expiry'] = this.expiry;
    } else {
      json[r'expiry'] = null;
    }
    return json;
  }

  /// Returns a new [Position] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static Position? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "Position[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "Position[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return Position(
        tradingSymbol: mapValueOfType<String>(json, r'tradingSymbol'),
        quantity: mapValueOfType<int>(json, r'quantity'),
        type: mapValueOfType<String>(json, r'type'),
        product: mapValueOfType<String>(json, r'product'),
        exchange: mapValueOfType<String>(json, r'exchange'),
        price: num.parse('${json[r'price']}'),
        optionType: mapValueOfType<String>(json, r'optionType'),
        strikePrice: num.parse('${json[r'strikePrice']}'),
        expiry: mapValueOfType<String>(json, r'expiry'),
      );
    }
    return null;
  }

  static List<Position> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <Position>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = Position.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, Position> mapFromJson(dynamic json) {
    final map = <String, Position>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = Position.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of Position-objects as value to a dart map
  static Map<String, List<Position>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<Position>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = Position.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

