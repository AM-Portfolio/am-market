import 'package:flutter/material.dart';

class CalendarHeatmap extends StatelessWidget {
  final String symbol;
  final int year;
  final Map<String, Map<int, double>> data;

  const CalendarHeatmap({
    Key? key,
    required this.symbol,
    required this.year,
    required this.data,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Text(
            '$symbol - $year Daily Returns',
            style: Theme.of(context).textTheme.titleLarge,
          ),
        ),
        Expanded(
          child: SingleChildScrollView(
            child: Wrap(
              spacing: 16.0,
              runSpacing: 16.0,
              alignment: WrapAlignment.center,
              children: List.generate(12, (index) {
                final monthIndex = index + 1;
                final monthName = _getMonthName(monthIndex);
                final monthData = data[monthName] ?? {};
                return _buildMonthGrid(context, monthIndex, monthName, monthData);
              }),
            ),
          ),
        ),
      ],
    );
  }

  String _getMonthName(int index) {
    const months = [
      'JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE',
      'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'
    ];
    return months[index - 1];
  }

  Widget _buildMonthGrid(BuildContext context, int monthIndex, String monthName, Map<int, double> monthData) {
    final daysInMonth = DateUtils.getDaysInMonth(year, monthIndex);
    final firstDayWeekday = DateTime(year, monthIndex, 1).weekday; // 1 = Mon, 7 = Sun
    
    // Grid: 7 columns (Mon-Sun). Rows depends on days.
    // Padding cells for start of month.
    
    return SizedBox(
      width: 300,
      height: 320,
      child: Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Column(
            children: [
              Text(
                monthName,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              _buildWeekDaysHeader(),
              const SizedBox(height: 4),
              Expanded(
                child: GridView.builder(
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 7,
                    mainAxisSpacing: 2,
                    crossAxisSpacing: 2,
                  ),
                  itemCount: daysInMonth + (firstDayWeekday - 1),
                  itemBuilder: (context, index) {
                    if (index < firstDayWeekday - 1) {
                      return const SizedBox();
                    }
                    final day = index - (firstDayWeekday - 1) + 1;
                    final value = monthData[day];
                    return _buildDayCell(day, value);
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildWeekDaysHeader() {
    const days = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      children: days.map((d) => Text(d, style: const TextStyle(fontSize: 10, color: Colors.grey))).toList(),
    );
  }

  Widget _buildDayCell(int day, double? value) {
    Color color = Colors.grey.shade200;
    if (value != null) {
      if (value > 0) {
        color = Colors.green.withOpacity((value / 5).clamp(0.2, 1.0)); // Cap at 5% for max opacity
      } else if (value < 0) {
        color = Colors.red.withOpacity((value.abs() / 5).clamp(0.2, 1.0));
      } else {
        color = Colors.grey;
      }
    }

    return Tooltip(
      message: value != null ? 'Day $day: ${value.toStringAsFixed(2)}%' : 'Day $day',
      child: Container(
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(2),
        ),
        child: Center(
          child: Text(
            '$day',
            style: TextStyle(
              fontSize: 10,
              color: value != null ? Colors.white : Colors.black54,
            ),
          ),
        ),
      ),
    );
  }
}
