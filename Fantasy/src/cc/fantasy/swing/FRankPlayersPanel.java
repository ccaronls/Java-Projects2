package cc.fantasy.swing;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.Team;
import cc.fantasy.model.TeamPlayer;
import cc.fantasy.util.Comparators;

public class FRankPlayersPanel extends FGridPanel implements ActionListener {

	TeamPlayer [] players;
	JLabel [] labels;
	Franchise franchise;
	Team team;
	
	FRankPlayersPanel(Position position, Team team) {
        super(3);
		this.franchise = position.getFranchise();
		this.team = team;
        List<TeamPlayer> pList = team.getPlayersForPosition(position.getName());
		players = pList.toArray(new TeamPlayer[pList.size()]);
		Arrays.sort(players, Comparators.getTeamPlayerRankComparator());
		for (int i=0; i<players.length; i++) {
			players[i].setRank(i+1);
		}
		labels = new JLabel[players.length];
		for (int i=0; i<players.length; i++) {
			add(new JLabel(String.valueOf(i+1)));
			labels[i] = new JLabel();
			add(labels[i]);
			JPanel buttons = new JPanel();
			add(buttons);
			buttons.setLayout(new FlowLayout());
			JButton button;
			if (i>0) {
				button = new JButton(TOP_LABEL);
				button.setActionCommand(String.valueOf(i));
				button.addActionListener(this);
				buttons.add(button);
				button = new JButton(UP_LABEL);
				button.setActionCommand(String.valueOf(i));
				button.addActionListener(this);
				buttons.add(button);
			}
			if (i<players.length-1) {
				button = new JButton(DOWN_LABEL);
				button.setActionCommand(String.valueOf(i));
				button.addActionListener(this);
				buttons.add(button);
			}
		}		
		refreshLabels();
	}
	
	void refreshLabels() {
		for (int i=0; i<players.length; i++) {
   			Player player = franchise.getPlayer(players[i].getPlayerId());
   			String name = Fantasy.instance.makePlayerNameString(player);
            labels[i].setText(name);            
		}
	}

	public void actionPerformed(ActionEvent ev) {
		
		JButton button = (JButton)ev.getSource();
		int index = Integer.parseInt(ev.getActionCommand());
		String label = button.getText();
		if (label.equals(UP_LABEL)) {
			// swap with player above index
			TeamPlayer t = players[index];
			players[index] = players[index-1];
			players[index-1] = t;
		} else if (label.equals(DOWN_LABEL)) {
			// swap with player below index
			TeamPlayer t = players[index];
			players[index] = players[index+1];
			players[index+1] = t;
		} else { // TOP
			// shift all above index down a slot and move index to the top 
			TeamPlayer t = players[index];
			for (int i=index; i>0; i--) {
				players[i] = players[i-1];
			}
			players[0] = t;
		}
		
		refreshLabels();
	}
	
	void commitChanges() {
		for (int i=0; i<players.length; i++) {
			players[i].setRank(i+1);
		}
	}
	
	private final static String UP_LABEL = "UP";
	private final static String DOWN_LABEL = "DOWN";
	private final static String TOP_LABEL = "TOP";
	
}
