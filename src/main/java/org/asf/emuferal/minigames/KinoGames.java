package org.asf.emuferal.minigames;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.players.Player;
import org.asf.emuferal.packets.xt.gameserver.minigames.MinigameCurrency;
import org.asf.emuferal.packets.xt.gameserver.minigames.MinigamePrize;

public class KinoGames {
    
    public static void OnJoin(Player plr){
       MinigameCurrency currency = new MinigameCurrency();
       currency.Currency = 206;
       plr.client.sendPacket(currency);
    }
    
    public static boolean HandleMessage(Player plr, String command, String data){
        XtReader rd = new XtReader(data);
		XtWriter pk = new XtWriter();
        pk.writeString("mm");
        pk.writeInt(3683);

        switch (command){
            case "loadGame": {
                int maxWager = 100;
                
                pk.writeString("loadGame");
                pk.writeInt(maxWager);
                pk.writeInt(0);
                break;
            }
            case "placeWager": {
                int wager = rd.readInt();
                
                pk.writeString("wagerAccepted");
                pk.writeInt(wager);
                break;
            }
            case "doubleUp": {
                int inAccept = rd.readInt();

                // Handler
                int DoubleUpAccepted = inAccept;
                //

                pk.writeString("doubleUpResults");
                pk.writeInt(DoubleUpAccepted);
                break;
            }
            case "gameCommand": {
                String gameCmd = rd.read();
                
                pk.writeString("gameCommand");
                HandleGameCommand(rd ,pk, gameCmd);
                break;
            }
            default: {
                return true;
            }
        }

        pk.writeString(""); // Data suffix
        String msg = pk.encode();
        plr.client.sendPacket(msg);

        return true;
    }

    public static void HandleGameCommand(XtReader rd, XtWriter pk, String gameCmd){
        switch (gameCmd){
            case "requestRoll": {
                String inDiceRequest = rd.read();
                int inTurnNumber = rd.readInt();

                // Handler
                String RollResults = "433211216454";
                //
                
                pk.writeString("rollResults");
                pk.writeString(RollResults);
                break;
            }
            case "requestCompare": {
                String playerDice = rd.read();
                String playerDice2 = rd.read();
                String playerDice3 = rd.read();

                // Handler
                String GameResults = "tie";
                //
                
                pk.writeString("compareResults");
                pk.writeString(GameResults);
                break;
            }
            case "requestTieRoll": {
                // Handler
                int TieRowIndex = 0;
                int PlayerValue = 3;
                int OpponentValue = 6;
                String TieTally = "win";
                //

                pk.writeString("tieResponse");
                pk.writeInt(TieRowIndex);
                pk.writeInt(PlayerValue);
                pk.writeInt(OpponentValue);
                pk.writeString(TieTally);
                break;
            }
        }

    }

    public static void GivePrize(Player plr){

    }

}

