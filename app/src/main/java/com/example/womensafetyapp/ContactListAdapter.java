package com.example.womensafetyapp;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ContactListAdapter extends ArrayAdapter<AllContacts> {


    public ContactListAdapter(Activity context , ArrayList<AllContacts> contactsObj) {
        super(context, 0 , contactsObj);

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final AllContacts  contactObj = (AllContacts) getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.contacts_list, parent, false);
        }

        TextView number = (TextView) convertView.findViewById(R.id.contact_number);
        TextView name = (TextView) convertView.findViewById(R.id.contact_name);

        number.setText(contactObj.getNumber().toString());
        name.setText(contactObj.getName());

        return convertView;
    }
}
