// CannonGameFragment.java
// CatapunFragment creates and manages a CannonView
package com.tmbn.catapun;

import android.app.Fragment;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.lang.Override;

public class CatapunFragment extends Fragment
{
   private Pantalla pantalla; // custom view to display the game

   // called when Fragment's view needs to be created
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState)
   {
      super.onCreateView(inflater, container, savedInstanceState);    
      View view = 
         inflater.inflate(R.layout.fragment_game, container, false);

      // get the CannonView
      pantalla = (Pantalla) view.findViewById(R.id.pantallaView);

      return view;
   }

   // set up volume control once Activity is created
   @Override
   public void onActivityCreated(Bundle savedInstanceState)
   {
      super.onActivityCreated(savedInstanceState);

      // allow volume keys to set game volume
      getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
   }

   // when MainActivity is paused, CatapunFragment terminates the game
   @Override
   public void onPause()
   {
      super.onPause(); 
      pantalla.stopGame(); // terminates the game
   } 
   
   // when MainActivity is paused, CatapunFragment releases resources
   @Override
   public void onDestroy()
   {
      super.onDestroy();
      pantalla.releaseResources();
   }
} // end class CatapunFragment

/*********************************************************************************
 * (C) Copyright 1992-2014 by Deitel & Associates, Inc. and * Pearson Education, *
 * Inc. All Rights Reserved. * * DISCLAIMER: The authors and publisher of this   *
 * book have used their * best efforts in preparing the book. These efforts      *
 * include the * development, research, and testing of the theories and programs *
 * * to determine their effectiveness. The authors and publisher make * no       *
 * warranty of any kind, expressed or implied, with regard to these * programs   *
 * or to the documentation contained in these books. The authors * and publisher *
 * shall not be liable in any event for incidental or * consequential damages in *
 * connection with, or arising out of, the * furnishing, performance, or use of  *
 * these programs.                                                               *
 *********************************************************************************/
