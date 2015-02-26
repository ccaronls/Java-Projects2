package cc.fantasy.swing;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.qt.datapicker.DatePicker;

public class FDatePicker extends JPanel implements ActionListener {

    private class ObservingTextField extends JTextField implements Observer {
        public void update(Observable o, Object arg) {
            Calendar calendar = (Calendar) arg;
            DatePicker dp = (DatePicker) o;
            //System.out.println("picked=" + dp.formatDate(calendar));
            date = dp.parseDate(dp.formatDate(calendar));
            setText(Fantasy.instance.getDateString(date));
        }
    };
    
    private DatePicker dp = null;
    private ObservingTextField textField = new ObservingTextField();    
    private JButton picker = new JButton("Choose");
    private Date date = null;
    
    FDatePicker() {
        this(new Date());
    }
    
    FDatePicker(Date date) {
    	if (date == null)
    		date = new Date();
    	this.date = date;    	
        setLayout(new GridLayout(1, 2));
        picker.addActionListener(this);
        textField.setColumns(10);
        textField.setText(Fantasy.instance.getDateString(date));
        textField.setEditable(false);
        textField.setToolTipText("This is a text field that implments Observer interface.");
        add(textField);
        add(picker);
    }
    
    public Date getDate() {
        return date;
    }

    public void actionPerformed(ActionEvent e) {
//      instantiate the DatePicker
        dp = new DatePicker(textField, Locale.US);
        // previously selected date
        dp.setSelectedDate(date);
        dp.start(textField);
    }
    
    
}
